include 'asynchttp_v1'
import java.text.SimpleDateFormat

/**
*  Ask Solar Panels
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

definition(
  name: "Ask Solar Panels",
  namespace: "rmurfster",
  author: "Richard Murphy",
  description: "Get information about the Solar Panels",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences 
{
  section("My Solar Panel Device") {
    input "theSolarPanel", "capability.energyMeter", required: true
  }
  section("Debugging") {
	input "logFilter", "number", title: "(0=off,1=ERROR only,2=<1+WARNING>,3=<2+INFO>,4=<3+DEBUG>,5=<4+TRACE>)",  
    	range: "0..5", description: "optional", defaultValue: 3
  }
}

def installed() 
{
  initialize()
}

def updated() 
{
  unsubscribe()
  initialize()
}

def initialize() 
{
  if (!state.accessToken) OAuthToken()

  // Output page for getting OAuth Information for including in AWS node.js
  traceEvent("Cheat sheet web page located at : ${getApiServerUrl()}/api/smartapps/installations/${app.id}/setup?access_token=${state.accessToken}", get_LOG_INFO())

  // Schedule Sunrise/Sunset updater since the device handler can't get Sunrise/Sunset events!
  def randomHour = (int)(Math.random() * 3)
  def randomMin = (int)(Math.random() * 60)
  def randomSec = (int)(Math.random() * 60)
  def dateStr = "2017-03-03 ${randomHour + ":" + randomMin + ":" + randomSec}"
  def sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
  sdf.setTimeZone(location.timeZone)
  def scheduleDate = sdf.parse(dateStr)  
  traceEvent("scheduleDate: ${scheduleDate}", get_LOG_DEBUG())
  schedule(scheduleDate, setSunriseSunSet)
  
  // Set Sunrise/Sunset now.
  setSunriseSunSet()  
}

mappings {
  path("/b") { action: [GET: "processBegin"] }
  path("/d") { action: [GET: "processDevice"] }
  path("/setup") { action: [GET: "setupData"] }
  //path("/u") { action: [GET: "getURLs"] }
  //path("/cheat") { action: [GET: "cheat"] }
  //path("/flash") { action: [GET: "flash"] }
}

def processBegin()
{
  traceEvent("processBegin()", get_LOG_INFO())
  
  def ver = params.Ver 		//Lambda Code Verisons
  def lVer = params.lVer		//Version number of Lambda code
  def date = params.Date		//Version date of Lambda code
  
  state.lambdaCode = "Lambda Code Version: ${ver} (${date})"
  def LambdaVersion = lVer as int
  def OOD = LambdaVersion < LambdaReq() ? "true" : "false"
  
  return ["OOD":OOD, "SmartAppVer": versionLong()]
}

//Version/Copyright/Information/Help-----------------------------------------------------------
private textAppName() { return "Ask Solar Panels" }	
private textVersion() {
    def version = "SmartApp Version: 1.0.0 (02/27/2017)", lambdaVersion = state.lambdaCode ? "\n" + state.lambdaCode : ""
    return "${version}${lambdaVersion}"
}
private versionInt(){ return 100 }
private LambdaReq() { return 100 }
private versionLong(){ return "1.0.0" }
private textCopyright() {return "Copyright © 2017 Richard Murphy" }
private textLicense() {
	def text = "Licensed under the Apache License, Version 2.0 (the 'License'); you may not use this file except in compliance with the License. You may obtain a copy of the License at\n\n"+
		"    http://www.apache.org/licenses/LICENSE-2.0\n\nUnless required by applicable law or agreed to in writing, software distributed under the License is distributed on an 'AS IS' BASIS, "+
		"WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License."
}
private textHelp() 
{ 
	def text = "This SmartApp provides an interface to control and query the Solar Panel Array configured within the SmartThings environment via the Amazon Echo ('Alexa')."
}

def setupData()
{
  def result ="<div style='padding:10px'><i><b><a href='http://aws.amazon.com' target='_blank'>Lambda</a> code variables:</b></i><br><br>var STappID = '${app.id}';<br>var STtoken = '${state.accessToken}';<br>"
  
  result += "var url='${getApiServerUrl()}/api/smartapps/installations/' + STappID + '/' ;<br><br><hr>"
  
	result += "<br><hr><br><i><b>URL of this setup page:</b></i><br><br>${getApiServerUrl()}/api/smartapps/installations/${app.id}/setup?access_token=${state.accessToken}<br><br><hr></div>"
  
	displayData(result)
}

def displayData(display)
{
	render(contentType: "text/html", 
    data: """<!DOCTYPE html><html><head><meta charset="UTF-8" /><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0"/></head><body style="margin: 0;">${display}</body></html>""")
}

def OAuthToken()
{
	try 
  {
    createAccessToken()
		traceEvent("Creating new Access Token", get_LOG_DEBUG())
	} catch (e) { traceEvent("Access Token not defined. OAuth may not be enabled. Go to the SmartApp IDE settings to enable OAuth.", get_LOG_ERROR()) }
}

def setSunriseSunSet()
{
  def sunRiseSet = getSunriseAndSunset()
  traceEvent("sunrise: ${sunRiseSet.sunrise}", get_LOG_DEBUG())
  traceEvent("sunset: ${sunRiseSet.sunset}", get_LOG_DEBUG())
  
  theSolarPanel.setSunriseSunset(sunRiseSet.sunrise, sunRiseSet.sunset)
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

def processDevice() 
{
  int LOG_ERROR= get_LOG_ERROR()
  int LOG_WARN=  get_LOG_WARN()
  int LOG_INFO=  get_LOG_INFO()
  int LOG_DEBUG= get_LOG_DEBUG()
  int LOG_TRACE= get_LOG_TRACE()
  
  def dev = params.Device.toLowerCase() 	//Label of device
  //def op = params.Operator				//Operation to perform
  //def numVal = params.Num     			//Number for dimmer/PIN type settings
  //def param = params.Param				//Other parameter (color)
  
  traceEvent("processDevice()", LOG_INFO)
  traceEvent("Dev: " + dev, LOG_DEBUG)
  //traceEvent("Op: " + op, LOG_DEBUG)
  //traceEvent("Num: " + numVal, LOG_DEBUG)
  //traceEvent("Param: " + param, LOG_DEBUG)

  def me = theSolarPanel

  //def dfCurrentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def dfCurrentTime = new java.text.SimpleDateFormat("MM/dd/yyyy KK:mm:ss a")
  dfCurrentTime.setTimeZone(location.timeZone)
  def dataTimeStamp = dfCurrentTime.parse(me.currentTimeStamp)
  def currentTime = new Date()
  def source = me.currentPowerSource == "Grid" ? "the grid" : "the PV array"
  def batteryPercentage = me.currentBattery.toString()
  def currentAcLoadAmps = me.currentAcLoadAmps.toString()
  def currentTransformerTemperature = me.currentTransformerTemperature.toString()  
  def bmkWatts = me.currentBmkVolts * me.currentBmkAmps

  def chargeStatus = me.currentChargeStatus.toString().toLowerCase()
  def chargeStatusVerb = (String) ((chargeStatus == "unknown") ? "" : chargeStatus.replace("bulk mppt", "absorb").replace("floatmppt", "float") + "ing")

  def durationRaw
  use (groovy.time.TimeCategory) {
      durationRaw = currentTime - dataTimeStamp
  }  
  
  String duration = durationRaw
  duration = duration.replaceAll("\\.[0-9]+", "")
  traceEvent("location.timeZone: ${location.timeZone}", LOG_DEBUG)
  traceEvent("dataTimeStamp: ${dataTimeStamp}", LOG_DEBUG)
  traceEvent("me.currentTimeStamp: ${me.currentTimeStamp}", LOG_DEBUG)
  traceEvent("currentTime: ${currentTime}", LOG_DEBUG)
  traceEvent("durationRaw: ${durationRaw}", LOG_DEBUG)
  traceEvent("duration: ${duration}", LOG_DEBUG)
  
  // Determine the Battery's Status.
  def batteryStatus = ""
  if (me.currentBmkAmps < 0)
  {
    batteryStatus = " and falling by ${Math.abs(me.currentBmkAmps)} amp" + (String)((Math.abs(me.currentBmkAmps) == 1) ? "" : "s")
  }
  else if (me.currentBmkAmps > 0 && (chargeStatus == "absorb" || chargeStatus == "bulk mppt"))
  {
    batteryStatus = " and absorbing by ${me.currentBmkAmps} amp" + (String)((me.currentBmkAmps == 1) ? "" : "s")
  }
  else
  {
    batteryStatus = " and ${chargeStatusVerb}"
  }
  
  def outputTxt = ""

  if (me.currentTransformerTemperature > 170)
  {
    outputTxt += "warning! the transformer's temperature is ${currentTransformerTemperature} degrees. "
  }  
  
  outputTxt += "as of ${duration} ago, " +
      "the batteries were at ${batteryPercentage}%${batteryStatus}. " +
  	  "the inverter is using ${source} to satisfy the AC demand of ${currentAcLoadAmps} amps, "
      
  if (chargeStatus == "unknown" || chargeStatus.contains("mppt"))
  {
    def chargeStatusText = chargeStatus.replace("floatmppt", "float MPPT").replace("bulkmppt", "bulk MPPT")
    def bmkWattsText = (String)((chargeStatus.contains("mppt")) ? "at ${me.currentBmkVolts} volts" : "")
    outputTxt += "the controller's charge mode is ${chargeStatusText} ${bmkWattsText}, "
  }
  
  outputTxt += "and, the total output for today is ${me.currentEnergy} killowatt" + (String)((me.currentEnergy == 1) ? "" : "s")
  
  traceEvent("Out: ${outputTxt}", LOG_DEBUG)
    
  return ["voiceOutput" : outputTxt]
}