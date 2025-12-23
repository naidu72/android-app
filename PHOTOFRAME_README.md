# Photo Slideshow App - Photoframe Branch

A beautiful Android photo slideshow app with customizable transitions and an iOS-style glass clock overlay.

## Features

### ✅ Core Functionality
- **Photo Slideshow**: Display photos in a continuous slideshow
- **Custom Photo Path**: Select any folder on your device containing photos
- **Configurable Interval**: Change slideshow speed (3, 5, 10, 15, or 30 seconds)
- **Multiple Transition Styles**: Choose from 6 different animation styles
- **iOS 26 Glass Clock**: Beautiful glassmorphism-style clock overlay in top-right corner
- **Persistent Settings**: Your preferences are saved automatically

### 🎨 Transition Styles
1. **Fade** - Smooth fade in/out between photos
2. **Slide Left** - Photos slide in from right, exit to left
3. **Slide Right** - Photos slide in from left, exit to right
4. **Slide Up** - Photos slide in from bottom, exit to top
5. **Slide Down** - Photos slide in from top, exit to bottom
6. **Rotate** - Photos rotate while transitioning

### ⏰ Clock Features
- Always visible in top-right corner
- Updates every second
- iOS 26 glassmorphism style with gradient background
- Remains visible during photo transitions
- Semi-transparent design that doesn't obstruct photos

## How to Use

### First Time Setup
1. **Launch the app** - You'll see the settings screen
2. **Select Photo Folder**:
   - Tap on "Photo Folder" card
   - Enter the full path to your photos folder
   - Examples:
     - `/storage/emulated/0/Pictures`
     - `/sdcard/DCIM/Camera`
     - `/storage/emulated/0/Download`
3. **Choose Interval** - Select how long each photo displays (3-30 seconds)
4. **Pick Transition Style** - Choose your favorite animation
5. **Tap "Start Slideshow"** - Begin the slideshow!

### During Slideshow
- **Tap anywhere** on screen to show/hide settings button
- **Tap settings button** (⚙️) to return to settings
- **Clock** always displays in top-right corner
- Photos automatically advance based on your interval setting

## Permissions

The app requires **READ_MEDIA_IMAGES** permission to access your photos. This permission is requested automatically when you first launch the app.

## Supported Image Formats
- JPEG (.jpg, .jpeg)
- PNG (.png)
- GIF (.gif)
- WebP (.webp)

## Technical Details

### Architecture
- Built with **Jetpack Compose** for modern UI
- Uses **DataStore** for persistent settings storage
- **Coil** library for efficient image loading
- **AnimatedContent** for smooth transitions

### File Structure
```
app/src/main/java/com/example/pi5_app01/
└── PhotoSlideshowActivity.kt  # Main activity with all features
```

### Key Components
- `PhotoSlideshowScreen()` - Main screen coordinator
- `SettingsScreen()` - Configuration UI
- `SlideshowView()` - Fullscreen slideshow display
- `PhotoWithTransition()` - Handles all transition animations
- `ClockOverlay()` - iOS-style glass clock component

## Customization

### Changing Default Settings
Edit `PhotoSlideshowActivity.kt`:
- Default interval: Change `intervalSeconds` initial value
- Default transition: Change `transitionStyle` initial value
- Clock position: Modify `ClockOverlay` alignment in `SlideshowView`

### Adding New Transition Styles
1. Add new enum value to `TransitionStyle`
2. Add case in `PhotoWithTransition()` when statement
3. Implement your animation using Compose Animation APIs

## Troubleshooting

### No Photos Found
- Verify the folder path is correct
- Ensure folder contains image files (jpg, png, gif, webp)
- Check that storage permission is granted

### Photos Not Loading
- Check file permissions
- Verify image files aren't corrupted
- Try a different folder path

### Clock Not Showing
- Ensure app has proper permissions
- Check that screen isn't in power-saving mode

## Future Enhancements (Possible)
- [ ] Support for video files
- [ ] Random photo order option
- [ ] Multiple folder selection
- [ ] Lock screen integration
- [ ] Home screen widget
- [ ] Network photo sources (Google Photos, etc.)

## Branch Information

This is the **photoframe** branch of the Pi5app01 project.

To switch to this branch:
```bash
git checkout photoframe
```

To merge back to main:
```bash
git checkout main
git merge photoframe
```

---

**Enjoy your photo slideshow! 📸✨**

