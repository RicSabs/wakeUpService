# wakeUpService
For Google Glass Enterprise Edition 2 an example app to turn the screen on again with a head gesture.

Tap on TouchPad to turn off the screen (takes a couple of seconds).
When tilting the head left, the screen should turn on.

A partial wake lock is used to keep reading sensor values.


### Problem
Screen is off + sensors are being read, not able to know if the hinge of the glass is closed.
This will result in the unwanted behaviour of the screen turning on again when the glass is tilted and the hinge is closed.
