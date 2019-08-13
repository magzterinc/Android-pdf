# Magzter-pdf
Android application for rendering pdf files

Forked from MuPDF library on 15-10-2012


To build/debug android build.

1) Download the android sdk, and install it. These instructions have been
written with r14 (the latest version at time of writing) of the SDK in mind;
other versions may give problems. On windows r14 unpacked as:

   C:\Program Files (x86)\Android\android-sdk

on Macos an older version installed as:

   /Library/android-sdk-mac_x86

Whatever directory it unpacks to, ensure that both the 'tools' and
'platform-tools' directories inside it have been added to your PATH.

2) If the SDK has not popped up a window already, bring up a shell, and run
'android' (or android.bat on cygwin/windows). You should now have a window
with a graphical gui for the sdk. From here you can install the different SDK
components for the different flavours of android. Download them all -
bandwidth and disk space are cheap, right?

3) In new versions of the GUI there is a 'Tools' menu from which you can
select 'Manage AVDs...'. In old versions, go to the Virtual Devices entry
on the right hand side. You need to create yourself an emulator image to
use. Click 'New...' on the right hand side and a window will appear. Fill
in the entries as follows:

     Name: FroyoEm
     Target: Android 2.2 - API Level 8
     CPU/ABI: ARM (armeabi)     (If this option exists)
     SD card: Size: 1024MiB
     Skin: Resolution: 480x756  (756 just fits my macbook screen, but 800 may
     	   	       		 be 'more standard')

Click 'Create AVD' (on old versions you may have to wait for a minute or
so while it is prepared. Now you can exit the GUI.

4) You will need a copy of the JDK installed. See
<http://www.oracle.com/technetwork/java/javase/downloads/>. When this
installs, ensure that JAVA_HOME is set to point to the installation
directory.

5) Now we are ready to build mupdf for Android. Check out a copy of MuPDF
(but you've done that already, cos you're reading this, right?).

6) Copy the android/local.properties.sample file to be
android/local.properties and edit the contents to match your setup.

