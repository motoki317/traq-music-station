# traq-music-station

Music bot for [traQ](https://github.com/traPtitech/traQ) written in Java 11.

Libraries used (see pom.xml for full dependencies):
- [LavaPlayer](https://github.com/sedmelluq/lavaplayer)
- [Selenium](https://www.selenium.dev/)
- [traq4j](https://github.com/motoki317/traq4j), [traq-bot4j](https://github.com/motoki317/traq-bot4j)
- [oggus](https://github.com/leonfancy/oggus)
- 

## Features

Some features are not complete due to traQ server not expecting
bots to join WebRTC rooms.

- Bots cannot access /ws API route to update their WebRTC states.
- Bots cannot access GET /webrtc, POST /webrtc/authenticate API routes, so it currently uses privileged user access tokens.
