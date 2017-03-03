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
  section("Debugging") {
	input "logFilter", "number", title: "(0=off,1=ERROR only,2=<1+WARNING>,3=<2+INFO>,4=<3+DEBUG>,5=<4+TRACE>)",  
    	range: "0..5", description: "optional", defaultValue: 3
  }
  
//  section("Title") {
    // TODO: put inputs here
//  }
}

def installed() {
  traceEvent("Installed with settings: ${settings}", get_LOG_INFO())

  initialize()
}

def updated() {
  traceEvent("Updated with settings: ${settings}", get_LOG_INFO())

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
  traceEvent("start: $start", get_LOG_DEBUG())
  traceEvent("end: $end", get_LOG_DEBUG())
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
  traceEvent("evaluate()", get_LOG_INFO())
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

  traceEvent("currentMode: $currentMode", get_LOG_DEBUG())
  traceEvent("theMotionSensor.currentMotion: $theMotionSensor.currentMotion", get_LOG_DEBUG())
  traceEvent("theThermostat.currentThermostatMode: $theThermostat.currentThermostatMode", get_LOG_DEBUG())
  traceEvent("currentTime: " + dfTimeDebug.format(currentTime), get_LOG_DEBUG())
  traceEvent("startTime: $startTime", get_LOG_DEBUG())
  traceEvent("endTime: $endTime", get_LOG_DEBUG())
  traceEvent("activeTime: $activeTime", get_LOG_DEBUG())
  traceEvent("validMode: $validMode", get_LOG_DEBUG())
  traceEvent("day: $day", get_LOG_DEBUG())
  traceEvent("days: $days", get_LOG_DEBUG())
  traceEvent("dayCheck: $dayCheck", get_LOG_DEBUG())
  traceEvent("theTemperature.currentTemperature: $theTemperature.currentTemperature", get_LOG_DEBUG())
  traceEvent("isThermostatSetToHeat: $isThermostatSetToHeat", get_LOG_DEBUG())
  //traceEvent("modes: $modes", get_LOG_DEBUG())
  
  //theMotionSensor.currentMotion == "active" && 
  if ( validMode && dayCheck && activeTime )
  {
    if (!isThermostatSetToHeat)
    {
      def logMessage = "Turning thermostat on..."
      traceEvent(logMessage, get_LOG_DEBUG())
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
      traceEvent(logMessage, get_LOG_DEBUG())
      //sendPush(logMessage)
      sendNotificationEvent(logMessage)
      theThermostat.off()
    }
  }
  
}

// event handlers
def theTemperatureHandler(evt)
{
  traceEvent("theTemperatureHandler: $evt, $settings", get_LOG_DEBUG())
  traceEvent("evt.value: $evt.value", get_LOG_DEBUG())
  traceEvent("theTemperature.currentTemperature: $theTemperature.currentTemperature", get_LOG_DEBUG())
  
  theThermostat.setTemperature(evt.integerValue)
  
  //evaluate()
}

def theThermostatHeatingSetpointHandler(evt)
{
  traceEvent("theThermostatHeatingSetpointHandler: $evt, $settings", get_LOG_DEBUG())
  traceEvent("evt.value: $evt.value", get_LOG_DEBUG())
}

/* Called when Thermostat turns heating on/off.
 * Valid values for evt: "heating", "cooling", "idle"
 */
def theThermostatOperatingStateHandler(evt)
{
  traceEvent("theThermostatOperatingStateHandler: $evt, $settings", get_LOG_DEBUG())
  traceEvent("evt.value: $evt.value", get_LOG_DEBUG())
  
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
  traceEvent("modeChangeHandler: $evt, $settings", get_LOG_DEBUG())
  traceEvent("evt.value: $evt.value", get_LOG_DEBUG())
  
  evaluate()
}

def theMotionSensorHandler(evt)
{
  traceEvent("theMotionSensorHandler: $evt, $settings", get_LOG_DEBUG())
  traceEvent("evt.value: $evt.value", get_LOG_DEBUG())
  evaluate()
}

def theSwitchSwitchHandler(evt)
{
  traceEvent("theSwitchSwitchHandler: $evt, $settings", get_LOG_DEBUG())
  traceEvent("evt.value: $evt.value", get_LOG_DEBUG())
}

def startTimeStateHandler()
{
  traceEvent("startTimeStateHandler", get_LOG_DEBUG())
  evaluate()
}

def endTimeStateHandler()
{
  traceEvent("endTimeStateHandler", get_LOG_DEBUG())
  evaluate()
}

// catchall
def event(evt)
{
  traceEvent("value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}", get_LOG_DEBUG())
}

// Supporting Routines.
def heaterSwitchControl(mode)
{
  if (mode == "on" && theSwitch.currentSwitch != "on")
  {
    def logMessage = "Turning heater on..."
    traceEvent(logMessage, get_LOG_DEBUG())
    //sendPush(logMessage)
    //sendNotificationEvent(logMessage)
    theSwitch.on()
  }
  else if (mode == "off" && theSwitch.currentSwitch != "off")
  {
    def logMessage = "Turning heater off..."
    traceEvent(logMessage, get_LOG_DEBUG())
    //sendPush(logMessage)
    //sendNotificationEvent(logMessage)
    theSwitch.off()
  }
}

private int get_LOG_ERROR() {return (int)1}
private int get_LOG_WARN()  {return (int)2}
private int get_LOG_INFO()  {return (int)3}
private int get_LOG_DEBUG() {return (int)4}
private int get_LOG_TRACE() {return (int)5}

def traceEvent(message, traceLevel=5)
{
  int LOG_ERROR= get_LOG_ERROR()
  int LOG_WARN=  get_LOG_WARN()
  int LOG_INFO=  get_LOG_INFO()
  int LOG_DEBUG= get_LOG_DEBUG()
  int LOG_TRACE= get_LOG_TRACE()

  int filterLevel = (int)(settings.logFilter ? settings.logFilter.toInteger() : LOG_WARN)

  if (filterLevel >= traceLevel) 
  {
    switch (traceLevel) {
      case LOG_ERROR:
        log.error "${message}"
        break
      case LOG_WARN:
        log.warn "${message}"
        break
      case LOG_INFO:
        log.info  "${message}"
        break
      case LOG_TRACE:
        log.trace "${message}"
        break
      case LOG_DEBUG:
      default:
        log.debug "${message}"
        break
    }  /* end switch*/              
  }
}
