Cloud Bowl Sample - Java Spring Boot
------------------------------------

To make changes, edit the `src/main/java/hello/Application.java` file.

Run Locally:
```
./mvnw spring-boot:run
```
------------------------------------
### Water Fight

arena sample:

![image](https://user-images.githubusercontent.com/39507830/235956735-928df182-225d-4c02-8f30-87aa65ae4a6e.png)

#### Introduction and goal

We will build a microservice that deploy it on GCP and talked to the arena manager the microservice's url.
The arena manager will send a HTTP POST request to our microservice and give us the current arena state, and we need to use it to implement our own strategy to beat others.

##### API request: 
the current arena state in an HTTP POST to the URL you provide us, with the following JSON structure:
```
{
  "_links": {
    "self": {
      "href": "https://YOUR_SERVICE_URL"
    }
  },
  "arena": {
    "dims": [4,3], // width, height
    "state": {
      "https://A_PLAYERS_URL": {
        "x": 0, // zero-based x position, where 0 = left
        "y": 0, // zero-based y position, where 0 = top
        "direction": "N", // N = North, W = West, S = South, E = East
        "wasHit": false,
        "score": 0
      }
      ... // also you and the other players
    }
  }
}
```

##### API response: 
indicate the bot's next move
```
\\ encoded as a single uppercase character of either:
F <- move Forward
R <- turn Right
L <- turn Left
T <- Throw
```
