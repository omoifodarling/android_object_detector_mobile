
# Object Detector Mobile (ODM) App

This software has three applications. It has a main activity from which other selections can be made of either picture or video.

No application in this software is run on the terminal save the installation of the software's library manager which is done with the Android Visual Visual Studio. It is wholly a mobile and on-screen appllication.

In all the appplication either takes a photograph (image) and displays the analysed image with bounding boxes around detected objects or it takes a live video stream and shows the analysed frames with bounding boxes around it.

## Picture DNN Analysis

This has two underlying activities from which an image from the gallery can be selected for DNN analysis or a photograph is taken by the phone and analysed. This is by using or clicking buttons on the touch screen of the mobile device.

So, the main option here are the Gallery or Snap. Gallery opens default gallery and gives rom for an image to be selected. After the selection the image is returned to the activity in an acrtivity for result and the URL is used to ,oad and pass the bitmap to the DNN for anylysis. 

When Snap is selected, the devices takes a phot and returns the URL of the photo in a get activity for result. This usrl is  used in loading the corresponding bitmap that is passeed to the DNN of openCV library as shown on the diectory structure.

However, to be able to run the application depending on the device, the corrsponding OpenCV Library (mobile) has to be installed in the device. After than the application localy installs the manager that suits the APK of the devices.

## Video DNN Analysis

On choosing this option, the activity opens the default camer and streams the frames to the activity that handles its processing. As noted, this is a very slow device and has more applications running in the background. This makes the DNN that requires dedicated threads to be pretty slow . However, the frames can be seen as procesed and displayed to the user. Stressed again, this is not a real-time application but gives a feel, visually of what happens in the real-time software.
