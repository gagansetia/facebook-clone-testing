# Facebook Clone — Maven Software Testing Project

Yeh ek self-contained Java + Maven testing project hai. Isme Facebook-style social network ki core features aur **125 automated test cases** hain:

- Registration, login, logout aur forgot-password
- Profile update aur privacy
- Friend request, accept, reject, unfriend aur block
- Post creation, edit, delete, like/unlike, comments
- Private messaging
- Search, notifications aur input validation

## VS Code mein kaise chalayein

1. VS Code mein yeh folder open karein.
2. **Extension Pack for Java** install karein (agar pehle se nahi hai).
3. JDK 17+ aur Maven 3.9+ available hon.
4. Terminal kholkar chalayein:

```bash
mvn clean test
```

Successful run mein `Tests run: 125, Failures: 0, Errors: 0` aayega. HTML/XML test reports `target/surefire-reports/` mein milengi.

## Facebook-style website chalana

Yeh project ek local **FaceConnect** website bhi deta hai: login, signup, feed, create-post, like/unlike, comments, share, friend requests, messages, notifications, search, Watch, Marketplace, Groups aur logout. Isse run karne ke liye terminal mein:

```bash
mvn spring-boot:run
```

Jab terminal mein `Started FacebookCloneApplication` aaye, browser mein yeh open karein:

```
http://localhost:8080
```

Demo login: `gagan` / `gagan`. Server band karne ke liye terminal mein `Ctrl + C` dabayein.

> Agar aapne server pehle se start kiya hua hai, `Ctrl + C` se stop karke `mvn spring-boot:run` dobara chalayein. Isse latest UI changes load honge.

## Selenium WebDriver + TestNG browser tests

Pehle FaceConnect server ko VS Code ke ek terminal mein running rakhein:

```bash
mvn spring-boot:run
```

Phir **dusre VS Code terminal** mein Selenium suite run karein:

```bash
mvn -Pselenium test
```

Yeh Chrome ko headless mode mein chala kar login screen, invalid password, password rule, valid login, post persistence after refresh, photo URL post, navigation aur account menu test karta hai. Visible Chrome chahiye ho to:

```bash
mvn -Pselenium test -Dheadless=false
```

Different port par app chal raha ho to:

```bash
mvn -Pselenium test -DbaseUrl=http://localhost:8081
```

### macOS setup (jab `mvn: command not found` aaye)

Pehle Homebrew ke through Java aur Maven install karein:

```bash
brew install openjdk@17 maven
```

Phir current terminal session mein Java set karke verify karein:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
java -version
mvn -version
```

Ab project folder mein jaakar tests run karein:

```bash
cd /Users/gagansetia/Documents/automation
mvn clean test
```

## Project structure

```
src/main/java/.../FacebookService.java  # Facebook-style business logic
src/test/java/.../FacebookServiceTest.java # 125 automated JUnit tests
src/test/java/.../FaceConnectSeleniumTest.java # Selenium WebDriver + TestNG UI tests
src/test/resources/testng.xml # TestNG suite definition
```

> Yeh academic/demo project hai; real Facebook ko automate nahi karta aur kisi real account/credential ki zarurat nahi hai.
