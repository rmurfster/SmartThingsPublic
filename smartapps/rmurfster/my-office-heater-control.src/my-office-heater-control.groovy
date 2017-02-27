/**
 *  My Office Heater Control
 *
 *  Copyright 2017 Richard Murphy
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  
 *  Temperature Sensor
 *    - Temperature below 'x' -
 *      - If State.motion == "motion" 
 *           and Mode == "home"
 *           -- Turn Heater On
 *    - Temperature above 'x' - Turn Heater Off
 *  
 *  Motion Sensor 
 *    - No motion for 'x' minutes - Set State.motion to "no-motion"
 *    - New motion - Set State.motion to "motion"
 *  
 *  Thermostat -
 *    - Temperature down - decrease temperature
 *    - Temperature up - increase temperature
 *    - Show sensor's temperature on Thermostat
 *    
 *  Time
 *    - Inbetween 'x' to 'x' - Set State to "allow"
 *      - Else - Set State to "disallow"
 *  
 *  Mode - 
 *    - Away / Home - Turn heater off
 *    - Home - Allow heater to turn on
 *   
 */
definition(
    name: "My Office Heater Control",
    namespace: "rmurfster",
    author: "Richard Murphy",
    description: "Control Office Heater Switch based on Virtual Thermostat Mode (thermostatMode.heat = On, thermostatMode.off = Off)",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences {
  section("Choose thermostat... ") {
    input "theThermostat", "capability.thermostat", required: true
  }
  section("Switch for office heater...") {
      input "theSwitch", "capability.switch", required: true
  }
  section("Temperature sensor...") {
      input "theTemperature", "capability.temperatureMeasurement", required: true
  }
  section("Motion sensor...") {
    input "theMotionSensor", "capability.motionSensor", required: true
	}
  section("Start time...") {
    input "startTime", "time", title: "Start Time?", required: true
  }
  section("End time...") {
    input "endTime", "time", title: "End Time?", required: true
  }
  section("On Which Days") {
        input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, 
          options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
  }
  section ("On Modes...") {
    input "onModes", "mode", title: "select mode(s)", multiple: true, required: true
  }
//  section("Title") {
    // TODO: put inputs here
//  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"

  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"

  unsubscribe()
  initialize()
}

def initialize() {
  // TODO: subscribe to attributes, devices, locations, etc.
  subscribe(theSwitch, "switch", theSwitchSwitchHandler)
  subscribe(location, "mode", modeChangeHandler)
  subscribe(theTemperature, "temperature", theTemperatureHandler)
  subscribe(theMotionSensor, "motionSensor", theMotionSensorHandler)

  //subscribe(theThermostat, "thermostatMode", theThermostatModeHandler)
  subscribe(theThermostat, "thermostatOperatingState", theThermostatOperatingStateHandler)
  subscribe(theThermostat, "thermostatHeatingSetpoint", theThermostatHeatingSetpointHandler)
  
  // Call evaluate function when start/end times expire.
  def start = timeToday(startTime, location.timeZone)
  def end = timeToday(endTime, location.timeZone)
  log.debug("start: $start")
  log.debug("end: $end")
  schedule(start.getTime() + 10000, startTimeStateHandler)
  schedule(end.getTime() + 10000, endTimeStateHandler)
  
  // Sync the Thermostat's Temperature with the current Temperature of the sensor.
  theThermostat.setTemperature(theTemperature.currentTemperature ?
      theTemperature.currentTemperature : 70)
  
  // Set Initial Thermostat Mode.
  evaluate()
}

/* 
 * This routine will evaluate if we need to turn on/off the thermostat
 * based on current Motion, Time, and Mode.
 */
def evaluate()
{
  log.debug "evaluate()"
  def currentTime = new Date()
  def currentMode = location.getMode()
  def validMode = currentMode in onModes
  
  def dfTimeDebug = new java.text.SimpleDateFormat("E MMM dd, yyyy HH:mm:ss")
  dfTimeDebug.setTimeZone(location.timeZone)
  
  def dfDay = new java.text.SimpleDateFormat("EEEE")
  dfDay.setTimeZone(location.timeZone)
  
  def day = dfDay.format(new Date())
  def dayCheck = days.contains(day)

  def activeTime = timeOfDayIsBetween(startTime, endTime, currentTime, location.timeZone)

  def isThermostatSetToHeat =( theThermostat.currentThermostatMode in ["heat", "emergency heat", "auto"])

  log.debug "currentMode: $currentMode"
  log.debug "theMotionSensor.currentMotion: $theMotionSensor.currentMotion"
  log.debug "theThermostat.currentThermostatMode: $theThermostat.currentThermostatMode"
  log.debug("currentTime: " + dfTimeDebug.format(currentTime))
  log.debug "startTime: $startTime"
  log.debug "endTime: $endTime"
  log.debug "activeTime: $activeTime"
  log.debug "validMode: $validMode"
  log.debug "day: $day"
  log.debug "days: $days"
  log.debug "dayCheck: $dayCheck"
  log.debug("theTemperature.currentTemperature: $theTemperature.currentTemperature")
  log.debug("isThermostatSetToHeat: $isThermostatSetToHeat")
  //log.debug "modes: $modes"
  
  //theMotionSensor.currentMotion == "active" && 
  if ( validMode && dayCheck && activeTime )
  {
    if (!isThermostatSetToHeat)
    {
      def logMessage = "Turning thermostat on..."
      log.debug(logMessage)
      //sendPush(logMessage)
      sendNotificationEvent(logMessage)
      theThermostat.heat()
    }
  }
  else
  {
    if (isThermostatSetToHeat)
    {
      def logMessage = "Turning thermostat off..."
      log.debug(logMessage)
      //sendPush(logMessage)
      sendNotificationEvent(logMessage)
      theThermostat.off()
    }
  }
  
}

// event handlers
def theTemperatureHandler(evt)
{
  log.debug "theTemperatureHandler: $evt, $settings"
  log.debug "evt.value: $evt.value"
  log.debug("theTemperature.currentTemperature: $theTemperature.currentTemperature")
  
  theThermostat.setTemperature(evt.integerValue)
  
  //evaluate()
}

def theThermostatHeatingSetpointHandler(evt)
{
  log.debug "theThermostatHeatingSetpointHandler: $evt, $settings"
  log.debug "evt.value: $evt.value"
}

/* Called when Thermostat turns heating on/off.
 * Valid values for evt: "heating", "cooling", "idle"
 */
def theThermostatOperatingStateHandler(evt)
{
  log.debug "theThermostatOperatingStateHandler: $evt, $settings"
  log.debug "evt.value: $evt.value"
  
  //evaluate()
  
  def isThermostatSetToHeat = (theThermostat.currentThermostatMode in ["heat", "emergency heat", "auto"])
  
  if (isThermostatSetToHeat && evt.value == "heating")
  {
    heaterSwitchControl("on")
  }
  else
  {
    heaterSwitchControl("off")
  }
}

def modeChangeHandler(evt)
{
  log.debug "modeChangeHandler: $evt, $settings"
  log.debug "evt.value: $evt.value"
  
  evaluate()
}

def theMotionSensorHandler(evt)
{
  log.debug "theMotionSensorHandler: $evt, $settings"
  log.debug "evt.value: $evt.value"
  evaluate()
}

def theSwitchSwitchHandler(evt)
{
  log.debug "theSwitchSwitchHandler: $evt, $settings"
  log.debug "evt.value: $evt.value"
}

def startTimeStateHandler()
{
  log.debug("startTimeStateHandler")
  evaluate()
}

def endTimeStateHandler()
{
  log.debug("endTimeStateHandler")
  evaluate()
}

// catchall
def event(evt)
{
  log.debug "value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}"
}

// Supporting Routines.
def heaterSwitchControl(mode)
{
  if (mode == "on" && theSwitch.currentSwitch != "on")
  {
    def logMessage = "Turning heater on..."
    log.debug(logMessage)
    //sendPush(logMessage)
    //sendNotificationEvent(logMessage)
    theSwitch.on()
  }
  else if (mode == "off" && theSwitch.currentSwitch != "off")
  {
    def logMessage = "Turning heater off..."
    log.debug(logMessage)
    //sendPush(logMessage)
    //sendNotificationEvent(logMessage)
    theSwitch.off()
  }
}

