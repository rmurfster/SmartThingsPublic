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

preferences {
	input("logFilter", "number",title: "(0=off,1=ERROR only,2=<1+WARNING>,3=<2+INFO>,4=<3+DEBUG>,5=<4+TRACE>)",  range: "0..5",
 		description: "optional" )
}

metadata {
  definition (name: "My Solar Status Handler", namespace: "rmurfster", author: "Richard Murphy") {
    capability "Actuator"
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
    attribute "sunrise", "string"
    attribute "sunset", "string"
    
    command "setSunriseSunset", ["string", "string"]
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
    valueTile("sunrise", "device.sunrise", decoration: "flat") {
      state "default", label:'Sunrise: ${currentValue}'
    }
    valueTile("sunset", "device.sunset", decoration: "flat") {
      state "default", label:'Sunset: ${currentValue}'
    }

    main (["battery"])
    details(["timeStamp","battery","powerSource","chargeStatus","power","energy","bmkVolts","bmkAmps","batteryTemperature","transformerTemperature","acLoadVolts", "acLoadAmps", "refresh","sunrise","sunset"])
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
  traceEvent("Parsing '${description}'", get_LOG_INFO())
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
  traceEvent("updated()", get_LOG_INFO())
  unschedule()
  installed()
}

/**
* Schedule a 1 minute poll of the device to refresh the
* tiles so the user gets the correct information.
*/
def startPoll() {
  traceEvent("startPoll()", get_LOG_INFO())
  unschedule()
  // Schedule 1 minute polling of speaker status (song average length is 3-4 minutes)
  def sec = Math.round(Math.floor(Math.random() * 60))
  def cron = "$sec 0/1 * * * ?" // every 1 min
  traceEvent("schedule('$cron', getSolarData)", get_LOG_DEBUG())
  schedule(cron, getSolarData)
}

def getSolarData()
{
  // See http://groovy-lang.org/processing-xml.html
  traceEvent("getSolarData()", get_LOG_INFO())

  def paramsInverter = [
    uri: "http://data.magnumenergy.com",
    path: "/MW1159",
    contentType: "xml"
      //requestContentType: 'application/xml'
  ]
  
  def authorization = "admin:k771229656"
  def encoded = authorization.bytes.encodeBase64()
  //traceEvent("authorization: ${authorization}", get_LOG_DEBUG())
  //traceEvent("encoded: ${encoded}", get_LOG_DEBUG())
  
  def paramsController = [
    uri: "http://murphyhome.mynetgear.com:8085",
    path: "/SolarService.svc/GetClassicData/",
    contentType: "json",
    headers: [Authorization: "Basic " +encoded]
  ]

  asynchttp_v1.get(processInverterResponse, paramsInverter)
  
  asynchttp_v1.get(processControllerResponse, paramsController)
}

void setSunriseSunset( sunrise, sunset )
{
  traceEvent("setSunriseSunset()", get_LOG_INFO())
  traceEvent("sunrise: ${sunrise}", get_LOG_DEBUG())
  traceEvent("sunset: ${sunset}", get_LOG_DEBUG())
  sendEvent(name : "sunrise", value: sunrise)
  sendEvent(name : "sunset", value: sunset)
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

def processInverterResponse(resp, data)
{
  int LOG_ERROR= get_LOG_ERROR()
  int LOG_WARN=  get_LOG_WARN()
  int LOG_INFO=  get_LOG_INFO()
  int LOG_DEBUG= get_LOG_DEBUG()
  int LOG_TRACE= get_LOG_TRACE()

  def temp
  
  traceEvent("processInverterResponse()", LOG_INFO)
  
  if (resp.hasError())
  {
    traceEvent("resp.hasError(): ${resp.hasError()}", LOG_ERROR)
    traceEvent("resp.status: ${resp.status}", LOG_ERROR)
    //traceEvent("errorData: ${response.errorData}", LOG_ERROR)
    return
  }  

  //def headers = resp.headers
  //headers.each { header, value ->
  //    traceEvent("$header: $value", LOG_DEBUG)
  //}

  //traceEvent("resp class: ${resp.getClass()}", LOG_DEBUG)
  //traceEvent("resp.data class: ${resp.data.class}", LOG_DEBUG)
  //traceEvent("response contentType: ${resp.contentType}", LOG_DEBUG)
  //traceEvent("response data: ${resp.data}", LOG_DEBUG)
  //traceEvent("resp: ${resp}", LOG_DEBUG)
  //traceEvent("data: ${data}", LOG_DEBUG)

  // Get xml data between <table> ... </table>
  def start = resp.data.indexOf("<table")
  def end = resp.data.indexOf("</table>", start)
  def newData = resp.data.substring(start, end+8)
  newData = newData.replace("&deg;", "")

  // Process the <table> xml data.
  def sResponse = new XmlSlurper().parseText(newData)
  //def sResponse = new XmlParser().parseText(newData)
  assert sResponse instanceof groovy.util.slurpersupport.GPathResult //groovy.util.Node

  def dataDate = sResponse.tr[0].td.span.text()[0..-5]
  traceEvent("dataDate: ${dataDate}", LOG_DEBUG)

  def percentCharge = sResponse.tr[2].td.span.text().substring(1).replaceAll("%", "").toInteger()
  traceEvent("percentCharge: ${percentCharge}", LOG_DEBUG)

  // "52.42 VDC @ 1.40 amps (73 watts)"
  def voltsAmps = sResponse.tr[3].td.span.text().substring(1)
  traceEvent("voltsAmps: ${voltsAmps}", LOG_DEBUG)
  temp = voltsAmps.split(" ")
  def bmkVolts = temp[0].toFloat()
  def bmkAmps = temp[3].toFloat()
  traceEvent("bmkVolts: ${bmkVolts}", LOG_DEBUG)
  traceEvent("bmkAmps: ${bmkAmps}", LOG_DEBUG)
  
  def batteryTemperature = sResponse.tr[11].td.text().split("/")[1].replaceAll("F", "").toInteger()
  traceEvent("batteryTemperature: ${batteryTemperature}", LOG_DEBUG)

  def transformerTemperature = sResponse.tr[12].td.text().split("/")[1].replaceAll("F", "").toInteger()
  traceEvent("transformerTemperatures: ${transformerTemperature}", LOG_DEBUG)

  // Get AC Load in volts / amps
  def acLoadVolts = -1
  def acLoadAmps = -1
  def acOut = sResponse.tr[14].td.text()
  traceEvent("acOut: ${acOut}", LOG_DEBUG)
  // "Approximately 120 volts AC @ 5 amps"
  if (acOut != "Inactive.")
  {
    temp = acOut.split(" ")
    acLoadVolts = temp[1].toInteger()
    acLoadAmps = temp[5].toInteger()
  	traceEvent("acLoadVolts: ${acLoadVolts}", LOG_DEBUG)
  	traceEvent("acLoadAmps: ${acLoadAmps}", LOG_DEBUG)
  }
  
  // Get AC In (if in Grid Mode)
  // "Approximately 4 amps"
  if (acLoadAmps == 0)
  {
  	acLoadAmps = sResponse.tr[15].td.text().split(" ")[1].toInteger()
  }

  // Get DC Load in Volts/Amps
  // Get AC Load in volts / amps
  def dcOut = sResponse.tr[17].td.span.text()
  traceEvent("dcOut: ${dcOut}", LOG_DEBUG)
  // "60.0 VDC @ 13 amps (780 watts)"
  temp = dcOut.split(" ")
  def dcLoadVolts = temp[0].toFloat()
  def dcLoadAmps = temp[3].toInteger()
  traceEvent("dcLoadVolts: ${dcLoadVolts}", LOG_DEBUG)
  traceEvent("dcLoadAmps: ${dcLoadAmps}", LOG_DEBUG)
  
  /* All we really care about from the DC Load is Amps
   * as it tells us the Load Source (Grid/Solar).
   */
  def powerSource = (dcLoadAmps > 0) ? "PV" : "Grid"
  traceEvent("powerSource: ${powerSource}", LOG_DEBUG)

  def isDaylight = true
  if (device.currentValue("sunrise"))
  {
    def sunrise = new Date(device.currentValue("sunrise"))
    def sunset = new Date(device.currentValue("sunset"))
    traceEvent("sunrise: ${sunrise}", LOG_DEBUG)
    traceEvent("sunset: ${sunset}", LOG_DEBUG)
    isDaylight = (boolean) timeOfDayIsBetween(sunrise, sunset, new Date(), location.timeZone)
  }
  traceEvent("isDaylight: ${isDaylight}", LOG_DEBUG)

  /* This is now handled in processControllerResponse()
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
  traceEvent("chargeStatus: ${chargeStatus}", LOG_DEBUG)
  */

  // Put data in our device registers
  //sendEvent(name : "timeStamp", value : dataDate)
//  sendEvent(name : "chargeStatus", value: chargeStatus)
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

def processControllerResponse(resp, data)
{
  int LOG_ERROR= get_LOG_ERROR()
  int LOG_WARN=  get_LOG_WARN()
  int LOG_INFO=  get_LOG_INFO()
  int LOG_DEBUG= get_LOG_DEBUG()
  int LOG_TRACE= get_LOG_TRACE()

  def temp
  
  traceEvent("processControllerResponse()", LOG_INFO)
  
  def headers = resp.headers
  headers.each { header, value ->
      traceEvent("$header: $value", LOG_DEBUG)
  }
  
  if (resp.hasError())
  {
    traceEvent("resp.hasError(): ${resp.hasError()}", LOG_ERROR)
    traceEvent("resp.status: ${resp.status}", LOG_ERROR)
    //traceEvent("errorData: ${response.errorData}", LOG_ERROR)
    
    sendEvent(name : "power", value : -1)
    sendEvent(name : "energy", value : -1)
    sendEvent(name : "chargeStatus", value: "Unknown")
    
    return
  }  

  //traceEvent("resp class: ${resp.getClass()}", LOG_DEBUG)
  //traceEvent("resp.data class: ${resp.data.class}", LOG_DEBUG)
  //traceEvent("response contentType: ${resp.contentType}", LOG_DEBUG)
  traceEvent("response data: ${resp.data}", LOG_DEBUG)
  traceEvent("resp: ${resp}", LOG_DEBUG)
  traceEvent("data: ${data}", LOG_DEBUG)
  traceEvent("resp.json: ${resp.json}", LOG_DEBUG)
  
  traceEvent("resp.json.error: ${resp.json.error}", LOG_DEBUG)
  traceEvent("resp.json.errorMessage: ${resp.json.errorMessage}", LOG_DEBUG)
  traceEvent("resp.json.batteryVolts: ${resp.json.batteryVolts}", LOG_DEBUG)
  traceEvent("resp.json.batteryCurrent: ${resp.json.batteryCurrent}", LOG_DEBUG)
  traceEvent("resp.json.pvVolts: ${resp.json.pvVolts}", LOG_DEBUG)
  traceEvent("resp.json.pvCurrent: ${resp.json.pvCurrent}", LOG_DEBUG)
  traceEvent("resp.json.wattsOut: ${resp.json.wattsOut}", LOG_DEBUG)
  traceEvent("resp.json.ampsOut: ${resp.json.ampsOut}", LOG_DEBUG)
  traceEvent("resp.json.pvEnergyToday: ${resp.json.pvEnergyToday}", LOG_DEBUG)
  traceEvent("resp.json.ahToday: ${resp.json.ahToday}", LOG_DEBUG)
  traceEvent("resp.json.floatMinutesToday: ${resp.json.floatMinutesToday}", LOG_DEBUG)
  traceEvent("resp.json.batteryTemp: ${resp.json.batteryTemp}", LOG_DEBUG)
  traceEvent("resp.json.chargeStatus: ${resp.json.chargeStatus}", LOG_DEBUG)
  traceEvent("resp.json.wbCurrent: ${resp.json.wbCurrent}", LOG_DEBUG)
  traceEvent("resp.json.aux1State: ${resp.json.aux1State}", LOG_DEBUG)
  traceEvent("resp.json.aux2State: ${resp.json.aux2State}", LOG_DEBUG)
  traceEvent("resp.json.lastUpdate: ${resp.json.lastUpdate}", LOG_DEBUG)
  traceEvent("resp.json.wbNetAmpHours: ${resp.json.wbNetAmpHours}", LOG_DEBUG)
  traceEvent("resp.json.wbAmpHoursRemaining: ${resp.json.wbAmpHoursRemaining}", LOG_DEBUG)
  traceEvent("resp.json.wbSOC: ${resp.json.wbSOC}", LOG_DEBUG)
  traceEvent("resp.json.pcBoardTemperature: ${resp.json.pcBoardTemperature}", LOG_DEBUG)
  traceEvent("resp.json.fetTemperature: ${resp.json.fetTemperature}", LOG_DEBUG)
  traceEvent("resp.json.absorbMinutesToday: ${resp.json.absorbMinutesToday}", LOG_DEBUG)
  traceEvent("resp.json.reasonLastResting: ${resp.json.reasonLastResting}", LOG_DEBUG)
  traceEvent("resp.json.nightMinutesNoPower: ${resp.json.nightMinutesNoPower}", LOG_DEBUG)

  def chargeStatus = resp.json.chargeStatus
  if (chargeStatus == "BulkMppt") chargeStatus = "Bulk MPPT"
  if (chargeStatus == "BulkFloat") chargeStatus = "Bulk Float"
  
  sendEvent(name : "power", value : resp.json.wattsOut)
  sendEvent(name : "energy", value : resp.json.pvEnergyToday)
  sendEvent(name : "chargeStatus", value: chargeStatus)
  sendEvent(name : "timeStamp", value: resp.json.lastUpdate)

  traceEvent("chargeStatus: ${chargeStatus}", LOG_DEBUG)
}
