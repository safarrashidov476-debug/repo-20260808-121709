# Build Notes

## FFmpegKit Dependency
- Original `com.arthenica:ffmpeg-kit-full:6.0-2` is RETIRED (removed from Maven Central April 2025)
- Use mirror: `io.github.xch168:ffmpeg-kit-full-gpl:1.0.2` (available on Maven Central)
- Source: https://central.sonatype.com/artifact/io.github.xch168/ffmpeg-kit-full-gpl
- Alternative: VideoKit-FFmpeg-Android from dotintent (https://github.com/dotintent/videokit-ffmpeg-android)

## FFmpegKit API (same as original arthenica)
- Package: com.arthenica.ffmpegkit
- FFmpegKit.execute(command) - sync execution
- ReturnCode.isSuccess(session.returnCode) - check success
- session.allLogsAsString - get error logs
- FFprobeKit.execute(command) - probe media info

## Android SDK Setup
- ANDROID_HOME=/home/ubuntu/android-sdk
- JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
- Gradle 8.2 at /tmp/gradle-8.2/bin/gradle
- Platform: android-34, Build tools: 34.0.0

## FFmpeg Audio Filter Commands (verified)
- Trim: -ss START -to END -c:a copy
- Echo: -af "aecho=in_gain:out_gain:delays:decays"
- Reverb: multi-tap aecho
- Delay: aecho with longer delay
- Pitch: asetrate=NEW_RATE,aresample=ORIG_RATE,atempo=CORRECTION
- Speed: atempo=FACTOR (0.5-2.0 range, chain for outside)
- Volume: volume=FACTOR
- Concat: -f concat -safe 0 -i list.txt -c copy
- Mix: -filter_complex "amix=inputs=N:duration=longest"
- Extract audio: -vn -acodec libmp3lame -b:a 192k
- Audio to video: -loop 1 -i img -i audio -c:v libx264 -tune stillimage -c:a aac -shortest
