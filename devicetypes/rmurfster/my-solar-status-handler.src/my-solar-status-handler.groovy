/**
*  My Solar Status Handler
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
*/

include 'asynchttp_v1'

metadata {
  definition (name: "My Solar Status Handler", namespace: "rmurfster", author: "Richard Murphy") {
    capability "Battery"
    capability "Energy Meter"
    capability "Power Meter"
    capability "Temperature Measurement"

    attribute "timeStamp", "string"
    attribute "batteryTemperature", "number"
    attribute "transformerTemperature", "number"
    attribute "acLoadVolts", "number"
    attribute "acLoadAmps", "number"
    attribute "powerSource", "string"
    attribute "bmkVolts", "number"
    attribute "bmkAmps", "number"
    attribute "chargeStatus", "string"
    attribute "sunrise", "Date"
    attribute "sunset", "Date"
    
    command "setSunriseSunset", ["Date", "Date"]
  }

  simulator {
    // TODO: define status and reply messages here
  }

  tiles {
    valueTile("timeStamp", "device.timeStamp", decoration: "flat") {
      state "Data Timestamp", label:'${currentValue}'
    }
    valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false) {
      state "default", label:'SOC: ${currentValue}%', unit:"", 
        backgroundColors:[
        [value: 50, color: "#BC2323"],
        [value: 60, color: "#D04E00"],
        [value: 80, color: "#F1D801"],
        [value: 95, color: "#6add46"],
        [value: 99, color: "#44b621"]
      ]
    }
    valueTile("powerSource", "device.powerSource", decoration: "flat") {
      state "default", label:'Load Source: ${currentValue}'
    }
    valueTile("chargeStatus", "device.chargeStatus", decoration: "flat") {
      state "default", label:'Charge Status: ${currentValue}'
    }
    valueTile("power", "device.power", decoration: "flat") {
      state "default", label:'Power: ${currentValue} W'
    }
    valueTile("energy", "device.energy", decoration: "flat") {
      state "default", label:'Energy: ${currentValue} kWh'
    }
    valueTile("bmkVolts", "device.bmkVolts", decoration: "flat") {
      state "default", label:'BMK Volts: ${currentValue}'
    }
    valueTile("bmkAmps", "device.bmkAmps", decoration: "flat") {
      state "default", label:'BMK Amps: ${currentValue}'
    }
    valueTile("batteryTemperature", "device.batteryTemperature", decoration: "flat") {
      state("default", label:'Battery Temp: ${currentValue}°', unit:"F",
        backgroundColors:[
              // Fahrenheit
              [value: 40, color: "#153591"],
              [value: 44, color: "#1e9cbb"],
              [value: 59, color: "#90d2a7"],
              [value: 74, color: "#44b621"],
              [value: 84, color: "#f1d801"],
              [value: 95, color: "#d04e00"],
              [value: 96, color: "#bc2323"]
              ])
    }
    valueTile("transformerTemperature", "device.transformerTemperature", decoration: "flat") {
      state("default", label:'Transformer Temp: ${currentValue}°', unit:"F",
        backgroundColors:[
              // Fahrenheit
              [value: 80, color: "#153591"],
              [value: 100, color: "#1e9cbb"],
              [value: 120, color: "#90d2a7"],
              [value: 140, color: "#44b621"],
              [value: 160, color: "#f1d801"],
              [value: 180, color: "#d04e00"],
              [value: 200, color: "#bc2323"]
              ])
    }
    valueTile("acLoadVolts", "device.acLoadVolts", decoration: "flat") {
      state "default", label:'AC Out Volts: ${currentValue}'
    }
    valueTile("acLoadAmps", "device.acLoadAmps", decoration: "flat") {
      state "default", label:'AC Out Amps: ${currentValue}'
    }
    standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
      state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    main (["battery"])
    details(["timeStamp","battery","powerSource","chargeStatus","power","energy","bmkVolts","bmkAmps","batteryTemperature","transformerTemperature","acLoadVolts", "acLoadAmps", "refresh"])
  }
}

/**
* Main point of interaction with things.
* This function is called by SmartThings Cloud with the resulting data from
* any action (see HubAction()).
*
* Conversely, to execute any actions, you need to return them as a single
* item or a list (flattened).
*
* @param data Data provided by the cloud
* @return an action or a list() of actions. Can also return null if no further
*         action is desired at this point.
*/
def parse(String description) {
  log.debug "Parsing '${description}'"
}

/**
* Called when the devicetype is first installed.
*
* @return action(s) to take or null
*/
def installed() {
  startPoll()
  getSolarData()
}

def updated()
{
  log.trace "updated()"
  unschedule()
  installed()
}

/**
* Schedule a 1 minute poll of the device to refresh the
* tiles so the user gets the correct information.
*/
def startPoll() {
  log.trace("startPoll")
  unschedule()
  // Schedule 1 minute polling of speaker status (song average length is 3-4 minutes)
  def sec = Math.round(Math.floor(Math.random() * 60))
  def cron = "$sec 0/1 * * * ?" // every 1 min
  log.debug "schedule('$cron', getSolarData)"
  schedule(cron, getSolarData)
}

def getSolarData()
{
  // See http://groovy-lang.org/processing-xml.html
  log.trace("getSolarData()")

  def params = [
    uri: "http://data.magnumenergy.com",
    path: "/MW1159",
    contentType: "xml"
      //requestContentType: 'application/xml'
  ]

  asynchttp_v1.get(processResponse, params)
}

void setSunriseSunset( sunrise, sunset )
{
  log.trace "setSunriseSunset()"
  log.debug "sunrise: ${sunrise}"
  log.debug "sunset: ${sunset}"
  sendEvent(name : "sunrise", value: sunrise)
  sendEvent(name : "sunset", value: sunset)
}

def processResponse(resp, data)
{
  def temp
  
  log.trace("processResponse()")
  log.debug "resp.status: ${resp.status}"
  log.debug "resp.hasError(): ${resp.hasError()}"

  //def headers = resp.headers
  //headers.each { header, value ->
  //    log.debug "$header: $value"
  //}

  //log.debug "resp class: ${resp.getClass()}"
  //log.debug "resp.data class: ${resp.data.class}"
  //log.debug "response contentType: ${resp.contentType}"
  //log.debug "response data: ${resp.data}"
  //log.debug "resp: ${resp}"
  //log.debug "data: ${data}"

  // Get xml data between <table> ... </table>
  def start = resp.data.indexOf("<table")
  def end = resp.data.indexOf("</table>", start)
  def newData = resp.data.substring(start, end+8)
  newData = newData.replace("&deg;", "")
  //log.debug "newData: ${newData}"

  // Process the <table> xml data.
  def sResponse = new XmlSlurper().parseText(newData)
  //def sResponse = new XmlParser().parseText(newData)

  assert sResponse instanceof groovy.util.slurpersupport.GPathResult //groovy.util.Node

  //log.debug "sResponse Name: ${sResponse.name()}"

  def dataDate = sResponse.tr[0].td.span.text()[0..-5]
  log.debug "dataDate: ${dataDate}"

  def percentCharge = sResponse.tr[2].td.span.text().substring(1).replaceAll("%", "").toInteger()
  log.debug "percentCharge: ${percentCharge}"

  // "52.42 VDC @ 1.40 amps (73 watts)"
  def voltsAmps = sResponse.tr[3].td.span.text().substring(1)
  log.debug "voltsAmps: ${voltsAmps}"
  temp = voltsAmps.split(" ")
  def bmkVolts = temp[0].toFloat()
  def bmkAmps = temp[3].toFloat()
  log.debug "bmkVolts: ${bmkVolts}"
  log.debug "bmkAmps: ${bmkAmps}"
  
  def batteryTemperature = sResponse.tr[11].td.text().split("/")[1].replaceAll("F", "").toInteger()
  log.debug "batteryTemperature: ${batteryTemperature}"

  def transformerTemperature = sResponse.tr[12].td.text().split("/")[1].replaceAll("F", "").toInteger()
  log.debug "transformerTemperatures: ${transformerTemperature}"

  // Get AC Load in volts / amps
  def acOut = sResponse.tr[14].td.text()
  log.debug "acOut: ${acOut}"
  // "Approximately 120 volts AC @ 5 amps"
  temp = acOut.split(" ")
  def acLoadVolts = temp[1].toInteger()
  def acLoadAmps = temp[5].toInteger()
  log.debug "acLoadVolts: ${acLoadVolts}"
  log.debug "acLoadAmps: ${acLoadAmps}"
  
  // Get AC In (if in Grid Mode)
  // "Approximately 4 amps"
  if (acLoadAmps == 0)
  {
  	acLoadAmps = sResponse.tr[15].td.text().split(" ")[1].toInteger()
  }

  // Get DC Load in Volts/Amps
  // Get AC Load in volts / amps
  def dcOut = sResponse.tr[17].td.span.text()
  log.debug "dcOut: ${dcOut}"
  // "60.0 VDC @ 13 amps (780 watts)"
  temp = dcOut.split(" ")
  def dcLoadVolts = temp[0].toFloat()
  def dcLoadAmps = temp[3].toInteger()
  log.debug "dcLoadVolts: ${dcLoadVolts}"
  log.debug "dcLoadAmps: ${dcLoadAmps}"
  
  /* All we really care about from the DC Load is Amps
   * as it tells us the Load Source (Grid/Solar).
   */
  def powerSource = (dcLoadAmps > 0) ? "Solar" : "Grid"
  log.debug "powerSource: ${powerSource}"

  //def sunrise = new Date(me.currentSunrise)
  //def sunset = new Date(me.currentSunset)
  def isDaylight = (boolean) (me.currentSunrise) ? timeOfDayIsBetween(me.currentSunrise, me.currentSunset, new Date(), location.timeZone) : true
  log.debug "sunrise: ${me.currentSunrise}"
  log.debug "sunset: ${me.currentSunset}"
  log.debug "isDaylight: ${isDaylight}"

  def chargeStatus = (String)(isDaylight ? "Bulk MPPT" : "Resting")
  if (isDaylight)
  {
    if (bmkVolts > (float) 59)
    {
      chargeStatus = "Absorb"
    }
    else if (bmkVolts > (float) 52.5 && percentCharge == 100)
    {
      chargeStatus = "Float"
    }  
  }
  log.debug "chargeStatus: ${chargeStatus}"

  // Put data in our device registers
  sendEvent(name : "timeStamp", value : dataDate)
  sendEvent(name : "chargeStatus", value: chargeStatus)
  sendEvent(name : "power", value : 0)
  sendEvent(name : "energy", value : 0)
  sendEvent(name : "battery", value : percentCharge)
  sendEvent(name : "bmkVolts", value: bmkVolts)
  sendEvent(name : "bmkAmps", value: bmkAmps)
  sendEvent(name : "batteryTemperature", value : batteryTemperature)
  sendEvent(name : "transformerTemperature", value : transformerTemperature)
  sendEvent(name : "temperature", value: transformerTemperature)
  sendEvent(name : "acLoadVolts", value: acLoadVolts)
  sendEvent(name : "acLoadAmps", value: acLoadAmps)
  sendEvent(name : "powerSource", value: powerSource)
}
