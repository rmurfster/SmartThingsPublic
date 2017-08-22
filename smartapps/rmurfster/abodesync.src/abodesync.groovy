/**
 *  Abode Syncronization
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
 *  Door Sensor
 *  
 *  Locks
 *  
 *  Motion Sensor 
 *  
 *  Time
 *    - Inbetween 'x' to 'x' - Set State to "allow"
 *      - Else - Set State to "disallow"
 *  
 *  Mode
 *   
 */
definition(
    name: "AbodeSync",
    namespace: "rmurfster",
    author: "Richard Murphy",
    description: "Synchronize SmartThings SHM, Mode, Doors, Locks with Abode Security",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  section("IFTTT Maker key") {
    input("iftttKey", "string", title: "IFTTT Key", description: "Your IFTTT Maker key", required: true, defaultValue: 'dDsn2XTPWUIGQWW_ZDeGLX')
  }
  section("Choose Locks... ") {
    input "myLocks", "capability.lock", multiple: true, required: true
  }
  section("Choose Garage Door... ") {
    input "myGarageDoor", "capability.doorControl", multiple: false, required: true
  }
  section("Choose Contact Sensors... ") {
    input "myContactSensors", "capability.contactSensor", multiple: true, required: true
  }
  section("Debugging") {
    input "logFilter", "number", title: "(0=off,1=ERROR only,2=<1+WARNING>,3=<2+INFO>,4=<3+DEBUG>,5=<4+TRACE>)",  
        range: "0..5", description: "optional", defaultValue: 3
  }
//	page(name: "pageIntegrateIFTTT")
//	page(name: "pageIntegrateIFTTTConfirm")
  
}

mappings {
	//path("/ifttt/:eventName") {action: [POST: "api_ifttt"]}
	path("/Abode-Lock-Changed")           {action: [POST: "abodeLockChanged"]}
	path("/Abode-ContactSensor-Changed")  {action: [POST: "abodeContactSensorChanged"]}
	path("/Abode-Mode-Changed")           {action: [POST: "abodeModeChanged"]}
	path("/Abode-GarageDoor-Changed")     {action: [POST: "abodeGarageDoorChanged"]}
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
  initializeAccessToken()
  initializeDeviceStates()  

  subscribe(myLocks, "lock", evaluateDeviceEvent)
  subscribe(myGarageDoor, "door", evaluateDeviceEvent)
  subscribe(location, "alarmSystemStatus", shmHandler)

  //subscribe(myContactSensors, "contact", evaluateDeviceEvent)
  //subscribe(location, "mode", modeChangeHandler)
}

def evaluateDeviceEvent(evt)
{
  traceEvent("evaluateDeviceEvent(evt: $evt)", get_LOG_INFO())
  //def currentTime = new Date()
  //def currentMode = location.getMode()
  
  //def dfTimeDebug = new java.text.SimpleDateFormat("E MMM dd, yyyy HH:mm:ss")
  //dfTimeDebug.setTimeZone(location.timeZone)
  
  //def dfDay = new java.text.SimpleDateFormat("EEEE")
  //dfDay.setTimeZone(location.timeZone)
  
  //traceEvent("currentTime: " + dfTimeDebug.format(currentTime), get_LOG_DEBUG())

  def currentState = state.currentState["$evt.displayName"]
  traceEvent("evt.value: $evt.value", get_LOG_DEBUG())
  traceEvent("currentState: $currentState", get_LOG_DEBUG())
  
  if (evt.value != currentState)
  {
    def makerEvent = buildIFTTTCommand(evt)
    
    if (makerEvent != null)
    {
      state.currentState["$evt.displayName"] = evt.value
      sendMakerEvent(makerEvent)
    }
  }
}

// SmartThings -> Abode
def shmHandler(evt)
{
  traceEvent("shmHandler: $evt, $settings", get_LOG_INFO())
  
  def alarmSystemStatus = location.currentState("alarmSystemStatus").value
  def currentState = state.currentState["alarmSystemStatus"]
  
  traceEvent("evt.value: $evt.value", get_LOG_DEBUG())
  traceEvent("alarmSystemStatus: $alarmSystemStatus", get_LOG_DEBUG())
  traceEvent("currentState: $currentState", get_LOG_DEBUG())

  def alarmValue = (evt.value == "off") ? "StandBy" :
                   (evt.value == "away") ? "Away" :
                   (evt.value == "stay") ? "Home" :
                   null

  if (alarmValue && evt.value != currentState)
  {
    def newEvt = [ value: alarmValue, 
                   device: evt.device, 
                   displayName: "Mode",
                   deviceId: evt.deviceId ]

    def makerEvent = buildIFTTTCommand(newEvt)
    
    if (makerEvent != null)
    {
      state.currentState["alarmSystemStatus"] = evt.value
      sendMakerEvent(makerEvent)
    }
  }
}

// catchall
def event(evt)
{
  traceEvent("value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}", get_LOG_ERROR())
}

def buildIFTTTCommand(evt) {
  traceEvent("buildIFTTTCommand(evt: $evt)", get_LOG_INFO())
  traceEvent("evt.value: $evt.value", get_LOG_DEBUG())
  traceEvent("evt.device: $evt.device", get_LOG_DEBUG())
  traceEvent("evt.displayName: $evt.displayName", get_LOG_DEBUG())
  traceEvent("evt.deviceId: $evt.deviceId", get_LOG_DEBUG())

  // Replace spaces with dashes in the Device Name.
  def deviceName = evt.displayName.replaceAll(' ','-')
  
  // Capitalize the command.
  def command = evt.value.split(/[^\w]/).collect { it.toLowerCase().capitalize() }.join("")
  
  def event = "Set-Abode-" + deviceName + "-" + command
  
  return event  
}

def getIftttKey() {
  return (settings.iftttKey)
	//if (parent) return parent.getIftttKey()
	//def module = atomicState.modules?.IFTTT
	//return (module && module.connected ? module.key : null)
}

private sendMakerEvent(event) {
  traceEvent("sendMakerEvent(event: $event)", get_LOG_INFO())
  def url = "https://maker.ifttt.com/trigger/${event}/with/key/" + getIftttKey()
  traceEvent("url: [$url]", get_LOG_DEBUG())

  httpGet(url){ response ->
    traceEvent("response.status: $response.status", get_LOG_DEBUG())
  }

	return true
}

def pageIntegrateIFTTT() {
	return dynamicPage(name: "pageIntegrateIFTTT", title: "IFTTT Integration", nextPage:  "pageIntegrateIFTTTConfirm") {
		section() {
			paragraph "AbodeSync integrates with IFTTT (IF This Then That) via the Maker channel to trigger immediate events to IFTTT. To enable IFTTT, please login to your IFTTT account and connect the Maker channel. You will be provided with a key that needs to be entered below", required: false
      input("iftttKey", "string", title: "Key", description: "Your IFTTT Maker key", required: true)
			href name: "", title: "IFTTT Maker channel", required: false, style: "external", url: "https://www.ifttt.com/maker", description: "tap to go to IFTTT and connect the Maker channel"
		}
	}
}

def pageIntegrateIFTTTConfirm() {
	if (testIFTTT()) {
		return dynamicPage(name: "pageIntegrateIFTTTConfirm", title: "IFTTT Integration") {
			section(){
				paragraph "Congratulations! You have successfully connected CoRE to IFTTT."
			}
		}
	} else {
		return dynamicPage(name: "pageIntegrateIFTTTConfirm",  title: "IFTTT Integration") {
			section(){
				paragraph "Sorry, the credentials you provided for IFTTT are invalid. Please go back and try again."
			}
		}
	}
}

def testIFTTT() {
	//setup our security descriptor
//	state.modules["IFTTT"] = [
//		key: settings.iftttKey,
//		connected: false
//	]
  //verify the key
  return httpGet("https://maker.ifttt.com/trigger/test/with/key/" + settings.iftttKey) { response ->
    if (response.status == 200) {
      if (response.data == "Congratulations! You've fired the test event")
        state.modules["IFTTT"].connected = true
      return true;
    }
    return false;
  }
  
	return false
}

def initializeDeviceStates()  
{
  traceEvent("initializeDeviceStates()", get_LOG_INFO())
  
  state.currentState = [:]
  
  myLocks.each {device ->
    traceEvent("$device.displayName: $device.currentLock", get_LOG_DEBUG())
    state.currentState["$device.displayName"] = "$device.currentLock"
  }

  myContactSensors.each {device ->
    traceEvent("$device.displayName: $device.currentContact", get_LOG_DEBUG())
    state.currentState["$device.displayName"] = "$device.currentContact"
  }
  
  traceEvent("$myGarageDoor.displayName: $myGarageDoor.currentContact", get_LOG_DEBUG())
  state.currentState["$myGarageDoor.displayName"] = "$myGarageDoor.currentContact"
  
  def alarmSystemStatus = location.currentState("alarmSystemStatus").value
  traceEvent("alarmSystemStatus: $alarmSystemStatus", get_LOG_DEBUG())
  state.currentState["alarmSystemStatus"] = location.currentState("alarmSystemStatus").value
}

def getDevice(deviceName)
{
  traceEvent("getDevice([$deviceName])", get_LOG_INFO())
  
  def foundDevice = null

  myLocks.each {device ->
    if (device.displayName == deviceName)
      foundDevice = device
  }  
  
  if (foundDevice == null)
  {
    myContactSensors.each {device ->
      if (device.displayName == deviceName)
        foundDevice = device
    }  
  }
  
  if (foundDevice == null)
  {
    if (myGarageDoor.displayName == deviceName)
      foundDevice = myGarageDoor
  }
  
  traceEvent("foundDevice: [$foundDevice]", get_LOG_DEBUG())
  
  return foundDevice
}

def abodeContactSensorChanged()
{
  doAbodeContactSensorChanged(true)
}

def abodeGarageDoorChanged()
{
  doAbodeContactSensorChanged(false)
}

def doAbodeContactSensorChanged(addSensorToName)
{
  /*
    https://graph-na02-useast1.api.smartthings.com:443/api/token/9a39f99a-a296-4341-9c66-246de51ad3a1/smartapps/installations/dc98d804-1b83-42f6-9004-f526bbb78c5b/Abode-ContactSensor-Changed

    {"value" : "open", "CreatedAt" : "{{CreatedAt}}", "DoorName" : "{{DoorName}}"}
  */
  traceEvent("abodeContactSensorChanged()", get_LOG_INFO())
  
	def data = request?.JSON
  traceEvent("value: [$data.value]", get_LOG_DEBUG())
  traceEvent("DoorName: [$data.DoorName]", get_LOG_DEBUG())
  traceEvent("CreatedAt: [$data.CreatedAt]", get_LOG_DEBUG())
  
  def sensorName = data.DoorName + (addSensorToName ? " Sensor" : "")
  
  def device = getDevice(sensorName)
  if (device == null)
  {
    traceEvent("Error. $data.DoorName is not a monitored device.", get_LOG_ERROR())
  }
  else
  {
    def currentState = state.currentState["$device.displayName"]
    traceEvent("device.currentContact: [$device.currentContact]", get_LOG_DEBUG())
    traceEvent("currentState: [$currentState]", get_LOG_DEBUG())

    if ((data.value != device.currentContact) || (data.value != currentState))
    {
      traceEvent("Changing state to $data.value", get_LOG_DEBUG())
      state.currentState["$device.displayName"] = data.value
      if (data.value == "closed")
      {
        device.close()
      }
      else if (data.value == "open")
      {
        device.open()
      }
      else
      {
        traceEvent("Error. Invalid value: [$data.value].", get_LOG_ERROR())
      }
    }
  }
}

def abodeLockChanged()
{
  /*
    state.endpoint: https://graph-na02-useast1.api.smartthings.com:443/api/token/9a39f99a-a296-4341-9c66-246de51ad3a1/smartapps/installations/dc98d804-1b83-42f6-9004-f526bbb78c5b/Abode-Lock-Changed

    {"value" : "locked", "CreatedAt" : "{{CreatedAt}}", "LockName" : "{{LockName}}"}
  */
  traceEvent("abodeLockChanged()", get_LOG_INFO())
  
  def data = request?.JSON
  traceEvent("value: [$data.value]", get_LOG_DEBUG())
  traceEvent("LockName: [$data.LockName]", get_LOG_DEBUG())
  traceEvent("CreatedAt: [$data.CreatedAt]", get_LOG_DEBUG())

  def device = getDevice(data.LockName)  
  if (device == null)
  {
    traceEvent("Error. $data.DoorName is not a monitored device.", get_LOG_ERROR())
  }
  else
  {
    def currentState = state.currentState["$device.displayName"]
    traceEvent("device.currentLock: [$device.currentLock]", get_LOG_DEBUG())
    traceEvent("currentState: [$currentState]", get_LOG_DEBUG())

    if ((data.value != device.currentLock) || (data.value != currentState))
    {
      traceEvent("Changing state to $data.value", get_LOG_DEBUG())
      state.currentState["$device.displayName"] = data.value
      if (data.value == "locked")
      {
        device.lock()
      }
      else if (data.value == "unlocked")
      {
        device.unlock()
      }
      else
      {
        traceEvent("Error. Invalid value: [$data.value].", get_LOG_ERROR())
      }
    }
  }
}

def abodeModeChanged()
{
  /*
    state.endpoint: https://graph-na02-useast1.api.smartthings.com:443/api/token/9a39f99a-a296-4341-9c66-246de51ad3a1/smartapps/installations/dc98d804-1b83-42f6-9004-f526bbb78c5b/Abode-Mode-Changed
    

    {"value" : "{{NewMode}}", "CreatedAt" : "{{CreatedAt}}"}
  */
  traceEvent("abodeModeChanged()", get_LOG_INFO())

  def data = request?.JSON
  def alarmSystemStatus = location.currentState("alarmSystemStatus").value
  def currentState = state.currentState["alarmSystemStatus"]
  def newAlarmSystemStatus = (data.value == "Gateway Disarmed - Standby") ? "off" :
                             (data.value == "Gateway Armed - Away") ? "away" :
                             (data.value == "Gateway Armed - Home") ? "stay" :
                             null
  def newLocationMode      = (data.value == "Gateway Disarmed - Standby") ? "Home" :
                             (data.value == "Gateway Armed - Away") ? "Away" :
                             (data.value == "Gateway Armed - Home") ? "Night" :
                             null

  traceEvent("value: [$data.value]", get_LOG_DEBUG())
  traceEvent("CreatedAt: [$data.CreatedAt]", get_LOG_DEBUG())
  traceEvent("currentState: [$currentState]", get_LOG_DEBUG())
  traceEvent("alarmSystemStatus: [$alarmSystemStatus]", get_LOG_DEBUG())
  traceEvent("newAlarmSystemStatus: [$newAlarmSystemStatus]", get_LOG_DEBUG())
  traceEvent("location.mode: [$location.mode]", get_LOG_DEBUG())
  traceEvent("newLocationMode: [$newLocationMode]", get_LOG_DEBUG())

  if ((newAlarmSystemStatus && (newAlarmSystemStatus != alarmSystemStatus)) || 
      (newAlarmSystemStatus != currentState))
  {
    traceEvent("Changing shm mode to $newAlarmSystemStatus", get_LOG_DEBUG())
    state.currentState["alarmSystemStatus"] = newAlarmSystemStatus
    sendLocationEvent(name: 'alarmSystemStatus', value: newAlarmSystemStatus)
  }
  
  if (newLocationMode && location.mode != newLocationMode)
  {
    traceEvent("Changing Location mode to $newLocationMode ", get_LOG_DEBUG())
    setLocationMode(newLocationMode)
  }
}

private initializeAccessToken() {
  traceEvent("initializeAccessToken()", get_LOG_INFO())
  
  if (!state.endpoint) {
	try {
      if (!state.accessToken) {
        createAccessToken()
      }
      state.endpoint = apiServerUrl("/api/token/${state.accessToken}/smartapps/installations/${app.id}/")
	} catch(e) {
      state.endpoint = null
	}
  }

  traceEvent("state.accessToken: [$state.accessToken]", get_LOG_DEBUG())
  traceEvent("state.endpoint: [$state.endpoint]", get_LOG_DEBUG())

  return state.endpoint
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

def api_ifttt() {
  /*
  Example:
  https://graph-na02-useast1.api.smartthings.com:443/api/token/9a39f99a-a296-4341-9c66-246de51ad3a1/smartapps/installations/dc98d804-1b83-42f6-9004-f526bbb78c5b/ifttt/:eventName
  */

  /*
     eventName: [Hi-Mommy]
     data: [[SwitchName: Sink Light]]
  */

  traceEvent("api_ifttt()", get_LOG_INFO())
  
  def data = request?.JSON
  def eventName = params?.eventName
  
  traceEvent("data: [$data]", get_LOG_DEBUG())
  traceEvent("eventName: [$eventName]", get_LOG_DEBUG())
}

