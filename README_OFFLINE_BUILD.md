# Running 100% Offline AI on Your Android Phone

This guide shows you how to run actual GGUF models (DeepSeek-R1, Llama 3.2, Qwen 2.5, SmolLM2) directly on your physical Android phone's CPU and RAM without any internet or cloud APIs.

---

## Prerequisites (on your PC)
1. **Android Studio** (Koala or Ladybug recommended) installed on your PC/Mac/Linux.
2. A physical Android phone with at least 4GB of RAM (Snapdragon 7/8 series or Dimensity recommended).
3. A USB cable to connect your phone to your PC.

---

## Step 1: Export This Project
1. In Google AI Studio Build (in your browser):
   - Click the **three dots menu (⋮)** or **Settings** icon in the top right.
   - Click **"Export Project as ZIP"** (or Push to GitHub).
2. Unzip the downloaded file on your computer.

---

## Step 2: Open in Android Studio & Install NDK
1. Open **Android Studio** and select **Open**, then choose the unzipped folder.
2. Go to **Tools > SDK Manager > SDK Tools** tab.
3. Check both:
   - **NDK (Side by side)** (version 26.x or newer)
   - **CMake** (version 3.22.1 or newer)
4. Click **Apply** and wait for Android Studio to download them.

---

## Step 3: Add `llama.cpp` Native Engine
To execute GGUF files directly on ARM64 mobile hardware:
1. Download precompiled `libllama.so` for Android ARM64, or use the official `llama.cpp` Android example:
   ```bash
   git clone https://github.com/ggerganov/llama.cpp
   ```
2. Place `libllama.so` into:
   ```
   app/src/main/jniLibs/arm64-v8a/libllama.so
   ```
3. In `app/build.gradle.kts`, ensure `jniLibs` is included:
   ```kotlin
   android {
       sourceSets {
           getByName("main") {
               jniLibs.srcDirs("src/main/jniLibs")
           }
       }
   }
   ```

---

## Step 4: Build & Install to Your Phone
1. Enable **Developer Options** and **USB Debugging** on your Android phone:
   - Go to **Settings > About Phone** -> Tap **Build Number** 7 times.
   - Go to **Settings > Developer Options** -> Enable **USB Debugging**.
2. Plug your phone into your PC via USB.
3. In Android Studio, select your physical phone in the top device dropdown.
4. Click the green **Run (▶)** button. The APK will compile, install, and launch on your phone.

---

## Step 5: Run 100% Offline with Zero Internet
1. Open the app on your phone.
2. Go to the **Models** tab and tap **Download** on a model suited for your phone's RAM:
   - **SmolLM2 135M**: ~100 MB (runs fast on any phone, even budget devices).
   - **DeepSeek R1 Distill 1.5B (Q4_K_M)**: ~1.1 GB (recommended for phones with 6GB+ RAM).
   - **Llama 3.2 1B (Q4_K_M)**: ~800 MB.
3. Turn on **Airplane Mode** on your phone (turn off Wi-Fi and Mobile Data).
4. Go to the **Chat** screen, select your downloaded model, and ask anything!
   - You will see live tokens/sec processed by your phone's processor.
   - For DeepSeek R1, the reasoning thinking chain `<think>` will process directly on your device.
