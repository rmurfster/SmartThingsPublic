/**
 *  Copyright 2015 SmartThings
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
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "RAM Simulated Thermostat", namespace: "rmurfster", author: "SmartThings/rmurfster") {
		capability "Thermostat"
		capability "Relative Humidity Measurement"
		capability "Sensor"
		capability "Actuator"

		command "tempUp"
		command "tempDown"
		command "heatUp"
		command "heatDown"
		command "coolUp"
		command "coolDown"
		command "setTemperature", ["number"]
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"thermostatMulti", type:"thermostat", width:6, height:4) {
			tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState("default", label:'${currentValue}', unit:"dF")
			}
			tileAttribute("device.temperature", key: "VALUE_CONTROL") {
				attributeState("VALUE_UP", action: "tempUp")
				attributeState("VALUE_DOWN", action: "tempDown")
			}
			tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'${currentValue}%', unit:"%")
			}
			tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
				attributeState("idle", backgroundColor:"#44b621")
				attributeState("heating", backgroundColor:"#ea5462")
				attributeState("cooling", backgroundColor:"#269bd2")
			}
			tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
				attributeState("off", label:'${name}')
				attributeState("heat", label:'${name}')
				attributeState("cool", label:'${name}')
				attributeState("auto", label:'${name}')
			}
			tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
				attributeState("default", label:'${currentValue}', unit:"dF")
			}
			tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
				attributeState("default", label:'${currentValue}', unit:"dF")
			}
		}

		valueTile("temperature", "device.temperature", width: 2, height: 2) {
			state("temperature", label:'${currentValue}', unit:"dF",
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
			)
		}
		standardTile("tempDown", "device.temperature", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'down', action:"tempDown"
		}
		standardTile("tempUp", "device.temperature", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'up', action:"tempUp"
		}

		valueTile("heatingSetpoint", "device.heatingSetpoint", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "heat", label:'${currentValue} heat', unit: "F", backgroundColor:"#ffffff"
		}
		standardTile("heatDown", "device.temperature", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'down', action:"heatDown"
		}
		standardTile("heatUp", "device.temperature", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'up', action:"heatUp"
		}

		valueTile("coolingSetpoint", "device.coolingSetpoint", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "cool", label:'${currentValue} cool', unit:"F", backgroundColor:"#ffffff"
		}
		standardTile("coolDown", "device.temperature", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'down', action:"coolDown"
		}
		standardTile("coolUp", "device.temperature", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'up', action:"coolUp"
		}

		standardTile("mode", "device.thermostatMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "off", label:'${name}', action:"thermostat.heat", backgroundColor:"#ffffff"
			state "heat", label:'${name}', action:"thermostat.cool", backgroundColor:"#ffa81e"
			state "cool", label:'${name}', action:"thermostat.auto", backgroundColor:"#269bd2"
			state "auto", label:'${name}', action:"thermostat.off", backgroundColor:"#79b821"
		}
		standardTile("fanMode", "device.thermostatFanMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "fanAuto", label:'${name}', action:"thermostat.fanOn", backgroundColor:"#ffffff"
			state "fanOn", label:'${name}', action:"thermostat.fanCirculate", backgroundColor:"#ffffff"
			state "fanCirculate", label:'${name}', action:"thermostat.fanAuto", backgroundColor:"#ffffff"
		}
		standardTile("operatingState", "device.thermostatOperatingState", width: 2, height: 2) {
			state "idle", label:'${name}', backgroundColor:"#ffffff"
			state "heating", label:'${name}', backgroundColor:"#ffa81e"
			state "cooling", label:'${name}', backgroundColor:"#269bd2"
		}

		main("thermostatMulti")
		details([
			"temperature","tempDown","tempUp",
			"mode", "fanMode", "operatingState",
			"heatingSetpoint", "heatDown", "heatUp",
			"coolingSetpoint", "coolDown", "coolUp",
		])
	}
}

def installed() {
	sendEventWithRetries(name: "temperature", value: 72, unit: "F")
	sendEventWithRetries(name: "heatingSetpoint", value: 70, unit: "F")
	sendEventWithRetries(name: "thermostatSetpoint", value: 70, unit: "F")
	sendEventWithRetries(name: "coolingSetpoint", value: 76, unit: "F")
	sendEventWithRetries(name: "thermostatMode", value: "off")
	sendEventWithRetries(name: "thermostatFanMode", value: "fanAuto")
	sendEventWithRetries(name: "thermostatOperatingState", value: "idle")
	sendEventWithRetries(name: "humidity", value: 53, unit: "%")
}

def parse(String description) {
  log.debug "description: $description"
}

def evaluate(temp, heatingSetpoint, coolingSetpoint) {
	log.debug "evaluate($temp, $heatingSetpoint, $coolingSetpoint)"
	def threshold = 1.0
	def current = device.currentValue("thermostatOperatingState")
	def mode = device.currentValue("thermostatMode")
    log.debug("currentThermostatOperatingState: $current")
    log.debug("currentThermostatMode: $mode")

	def heating = false
	def cooling = false
	def idle = false
	if (mode in ["heat","emergency heat","auto"]) {
		if (heatingSetpoint - temp >= threshold) {
			heating = true
			sendEventWithRetries(name: "thermostatOperatingState", value: "heating")
		}
		else if (temp - heatingSetpoint >= threshold) {
			idle = true
		}
		sendEventWithRetries(name: "thermostatSetpoint", value: heatingSetpoint)
	}
  
	if (mode in ["cool","auto"]) {
		if (temp - coolingSetpoint >= threshold) {
			cooling = true
			sendEventWithRetries(name: "thermostatOperatingState", value: "cooling")
		}
		else if (coolingSetpoint - temp >= threshold && !heating) {
			idle = true
		}
		sendEventWithRetries(name: "thermostatSetpoint", value: coolingSetpoint)
	}
  
	else {
		idle = true
		sendEventWithRetries(name: "thermostatSetpoint", value: heatingSetpoint)
	}
  
	if (idle && !heating && !cooling) {
		sendEventWithRetries(name: "thermostatOperatingState", value: "idle")
	}
    
    log.debug("heating: $heating")
    log.debug("cooling: $cooling")
    log.debug("idle: $idle")
    
    
}

def setHeatingSetpoint(Double degreesF) {
	log.debug "setHeatingSetpoint($degreesF)"
	sendEventWithRetries(name: "heatingSetpoint", value: degreesF)
	evaluate(device.currentValue("temperature"), degreesF, device.currentValue("coolingSetpoint"))
}

def setCoolingSetpoint(Double degreesF) {
	log.debug "setCoolingSetpoint($degreesF)"
	sendEventWithRetries(name: "coolingSetpoint", value: degreesF)
	evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), degreesF)
}

def setThermostatMode(String value) {
  log.debug("setThermostatMode: $value")
	sendEventWithRetries(name: "thermostatMode", value: value)
	evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def setThermostatFanMode(String value) {
	sendEventWithRetries(name: "thermostatFanMode", value: value)
}

def off() {
  log.debug("off()")
	sendEventWithRetries(name: "thermostatMode", value: "off")
	evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def heat() {
  log.debug("heat()")
	sendEventWithRetries(name: "thermostatMode", value: "heat")
	evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def auto() {
  log.debug("auto()")
	sendEventWithRetries(name: "thermostatMode", value: "auto")
	evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def emergencyHeat() {
	sendEventWithRetries(name: "thermostatMode", value: "emergency heat")
	evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def cool() {
  log.debug("cool()")
	sendEventWithRetries(name: "thermostatMode", value: "cool")
	evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def fanOn() {
	sendEventWithRetries(name: "thermostatFanMode", value: "fanOn")
}

def fanAuto() {
	sendEventWithRetries(name: "thermostatFanMode", value: "fanAuto")
}

def fanCirculate() {
	sendEventWithRetries(name: "thermostatFanMode", value: "fanCirculate")
}

def poll() {
	null
}

def tempUp() {
	def ts = device.currentState("temperature")
	def value = ts ? ts.integerValue + 1 : 72
	sendEventWithRetries(name:"temperature", value: value)
	evaluate(value, device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def tempDown() {
	def ts = device.currentState("temperature")
	def value = ts ? ts.integerValue - 1 : 72
	sendEventWithRetries(name:"temperature", value: value)
	evaluate(value, device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def setTemperature(value) {
	def ts = device.currentState("temperature")
	sendEventWithRetries(name:"temperature", value: value)
	evaluate(value, device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def heatUp() {
	def ts = device.currentState("heatingSetpoint")
	def value = ts ? ts.integerValue + 1 : 68
	sendEventWithRetries(name:"heatingSetpoint", value: value)
	evaluate(device.currentValue("temperature"), value, device.currentValue("coolingSetpoint"))
}

def heatDown() {
	def ts = device.currentState("heatingSetpoint")
	def value = ts ? ts.integerValue - 1 : 68
	sendEventWithRetries(name:"heatingSetpoint", value: value)
	evaluate(device.currentValue("temperature"), value, device.currentValue("coolingSetpoint"))
}


def coolUp() {
	def ts = device.currentState("coolingSetpoint")
	def value = ts ? ts.integerValue + 1 : 76
	sendEventWithRetries(name:"coolingSetpoint", value: value)
	evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), value)
}

def coolDown() {
	def ts = device.currentState("coolingSetpoint")
	def value = ts ? ts.integerValue - 1 : 76
	sendEventWithRetries(name:"coolingSetpoint", value: value)
	evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), value)
}

def sendEventWithRetries(Map properties)
{
  log.debug("sendEventWithRetries(name: $properties.name)")
  def isEventSent = false
  def maxRetries = 3
  
  for (def numRetries = 0 ; !isEventSent && numRetries < maxRetries ; numRetries++)
  {
    try
    {
      sendEvent(properties)
      isEventSent = true
    }
    catch (e)
    {
      log.debug("sendEvent(name: $properties.name, ...) failed.  Retrying (${numRetries+1})...")
      log.debug("e: $e")
      for (def i = 0 ; i < 15000 ; i++) {} // Poor man's pause :) .. Getting error using sleep() or pause()!!
    }
  }
  if (numRetries == maxRetries)
  {
    def message = "sendEvent(name: $properties.name, ...) FAILED!"
    log.debug(message)
    sendPush(message)
  }
}
