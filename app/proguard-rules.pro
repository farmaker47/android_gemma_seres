# MediaPipe osztályok megőrzése (ne törölje ki R8)
-keep class com.google.mediapipe.** { *; }

# Ne dobjon warningot MediaPipe miatt
-dontwarn com.google.mediapipe.**

