/**
 *  Simulated Door Lock
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
	definition (name: "My Simulated Door Lock", namespace: "rmurfster", author: "rmurfster") {
		capability "Actuator"
    	capability "Lock"
		//capability "Sensor"
    //capability "Door Control"
	}

	simulator {
		
	}

	tiles {
		standardTile("toggle", "device.lock", width: 2, height: 2) {
			state("locked", label:'${name}', action:"lock.unlock", icon:"st.locks.lock.locked", backgroundColor:"#79b821")
			state("unlocked", label:'${name}', action:"lock.lock", icon:"st.locks.lock.unlocked", backgroundColor:"#ffa81e")
			
		}
		standardTile("unlock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'unlock', action:"lock.unlock", icon:"st.locks.lock.unlock"
		}
		standardTile("lock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'lock', action:"lock.lock", icon:"st.locks.lock.locked"
		}

		main "toggle"
		details(["toggle", "unlock", "lock"])
	}
}

def parse(String description) {
	log.trace "parse($description)"
}

def unlock() {
    log.debug("hey dad, I'm unlocked!")
    sendEvent(name: "lock", value: "unlocked")
}

def lock() {
    log.debug("hey mom, I'm locked!")
    sendEvent(name: "lock", value: "locked")
}
