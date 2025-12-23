# Pi5 App - Beginner's Guide

Welcome to your first Android app! This is a simple counter app that will help you learn the basics of Android development.

## What This App Does

This app displays a number that starts at 0. You can:
- **Tap the "+" button** to increase the number
- **Tap the "-" button** to decrease the number  
- **Tap the "Reset" button** to set it back to 0

## Key Concepts You're Learning

### 1. **State Management**
```kotlin
var count by remember { mutableIntStateOf(0) }
```
- `count` stores the current number
- When `count` changes, the screen automatically updates
- This is called "reactive" programming

### 2. **Composable Functions**
Functions marked with `@Composable` create parts of your screen:
- `CounterApp()` - The main screen
- `Text()` - Displays text
- `Button()` - Creates a clickable button

### 3. **Layouts**
- `Column` - Arranges items vertically (top to bottom)
- `Row` - Arranges items horizontally (left to right)
- `Spacer` - Adds empty space between items

### 4. **User Interaction**
```kotlin
Button(onClick = { count++ })
```
- `onClick` is what happens when you tap the button
- `count++` means "add 1 to count"

## Project Structure

```
app/src/main/
├── java/com/example/pi5_app01/
│   └── MainActivity.kt          ← Your main app code (this file!)
└── res/
    ├── values/
    │   └── strings.xml           ← App name and text strings
    └── mipmap-*/                 ← App icons
```

## How to Run the App

1. **In Android Studio:**
   - Click the green "Run" button (▶️) at the top
   - Or press `Shift + F10`

2. **Choose where to run:**
   - Select an Android device/emulator
   - The app will install and launch automatically

## Next Steps to Learn

### Try These Modifications:

1. **Change the starting number:**
   ```kotlin
   var count by remember { mutableIntStateOf(10) } // Start at 10 instead of 0
   ```

2. **Change the step size:**
   ```kotlin
   Button(onClick = { count += 5 }) // Increase by 5 instead of 1
   ```

3. **Add a maximum limit:**
   ```kotlin
   Button(onClick = { if (count < 100) count++ }) // Can't go above 100
   ```

4. **Change colors:**
   - Look in `ui/theme/Color.kt` to see available colors
   - Try changing `MaterialTheme.colorScheme.primary` to other colors

5. **Add more buttons:**
   - Try adding a "×2" button that doubles the count
   - Or a "÷2" button that halves it

## Common Questions

**Q: What is Kotlin?**  
A: Kotlin is the programming language used for Android apps. It's similar to Java but easier to learn.

**Q: What is Jetpack Compose?**  
A: It's the modern way to build Android user interfaces. Instead of XML files, you write code that looks like the UI.

**Q: What does `@Composable` mean?**  
A: It tells Android "this function creates part of the screen." When the data changes, Android automatically redraws that part.

**Q: How do I add more screens?**  
A: You'll need to learn about Navigation. For now, focus on making this one screen better!

## Resources for Learning

- [Android Developers Official Docs](https://developer.android.com)
- [Kotlin Basics](https://kotlinlang.org/docs/basic-syntax.html)
- [Jetpack Compose Tutorial](https://developer.android.com/jetpack/compose/tutorial)

## Need Help?

- Check the code comments in `MainActivity.kt` - they explain what each part does
- Experiment! Change things and see what happens
- Don't worry about breaking things - you can always undo changes

Happy coding! 🚀
