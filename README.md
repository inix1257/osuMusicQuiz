# osu! Music Quiz

osu! Music Quiz is a web-based music guessing game inspired by [animemusicquiz](https://animemusicquiz.com/).

The goal of this game is to guess the correct [osu!](https://osu.ppy.sh/) beatmap based on a few clues such as audio and background preview.

Currently being hosted on https://osumusicquiz.com/.

# Contribution
If you want to contribute, you need to edit and fill out 2 local variable files first.

`frontend/.env.example`:
- `VUE_APP_API_URL`: main backend url endpoint of your applicaiton
- `VUE_APP_OSU_CLIENT_ID`: osu client id
- `VUE_APP_OSU_REDIRECT_URI`: osu callback uri
- `VUE_APP_DEBUG`: for debug purpose, should be either `true` or `false`
- `VUE_APP_OSU_ADMINUSERID`: for debug and management purpose, put your osu userid


rename this file to `.env.live` for live server, `.env.local` for dev server

`src/main/resources/application.example.properties`:
- `spring.datasource.url`: mysql url
- `spring.datasource.username`: mysql username
- `spring.datasource.password`: mysql password

- `osu.clientId`: osu client id
- `osu.clientSecret`: osu client secret
- `osu.redirectUri`: osu callback uri
- `osu.apiKey`: osu api v1 key
- `osu.adminUserId`: admin osu userid for management purpose
- `mainUrl`: main backend api endpoint of your server
