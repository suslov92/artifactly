/*
 * Copyright 2011 Thomas Amsler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package org.artifactly.client;

public interface ApplicationConstants {

	public static final String NOTIFICATION_INTENT_KEY = "message";
	public static final String PREFERENCE_RADIUS = "radius";
	public static final String PREFERENCE_BACKGROUND_COLOR = "background-color";
	public static final String PREFERENCE_SOUND_NOTIFICATION = "sound-notification";
	public static final int PREFERENCE_RADIUS_DEFAULT = 1000;
	public static final String PREFERENCE_BACKGROUND_COLOR_DEFAULT = "#ADDFFF";
	public static final boolean PREFERENCE_SOUND_NOTIFICATION_DEFAULT = true;
	public static final String LOCATION_UPDATE_INTENT = "org.artifactly.client.service.LocationUpdateIntent";
}
