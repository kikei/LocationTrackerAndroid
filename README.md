# LocationTrackerAndroid

Always track your location.

## Source

Source code is managed on GitHub. The URL is:

    https://github.com/kikei/LocationTrackerAndroid

## Dependencies

This project depends on okhttp3. Download okhttp-3.2.0.jar from
"http://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/3.2.0/okhttp-3.2.0.jar" and put it into app/libs.

## Settings

You have to set some values for running this application.

### Google Maps API Key

Put your Google Maps API Key into "/app/src/main/res/values/keys.xml".
This setting is required to show google map fragment.
XML example is:

    <resources>
        <string name="google_maps_api_key">PutYourGoogleMapsAPIKeyHere</string>
    </resources>
