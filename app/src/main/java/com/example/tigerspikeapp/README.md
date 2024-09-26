# Android Studio Project Landmark Remark

An application allow users use Google map to note into map

## A brief outline of my approach

I'm using Firebase Realtime DB to save all login info of users, 
and save all note content, address, lat lng of users

I'm using GoogleMap api to show map and create all note marker point

## The time I spent on which aspects of the application

(0.2 day) For integrate Firebase and GoogleMap into application
(0.5 day) For create login, register function with Firebase
(1.5 day) For create all functions in MainActivity: Show note, 
view detail note, search, add note,...
(0.3 day) For test app and fix bugs

## Config

MAPS_API_KEY is Google map API key, create it at: https://console.cloud.google.com/apis/credentials
Fill it into: local.properties file and MainActivity.kt file
