# BruhBot
A terrible Discord bot.

## Why?
Some people have asked me for the source code. I initially had the bot repository private, stowed away like one might sweep filth under the rug. However, I decided it might be a good idea to let people observe this ancient creation, hopefully so that they will learn how *not* to program.

## What should I know?
This bot was born in 2020, as a way for me to learn the JDA library. My initial objective was to have a simple bot that replied to messages in my Discord server with funny pictures. As we all know, however, with great power comes great responsibility. I unfortunately had a power trip, and decided to turn it into a social experiment of how horrible a user experience I could force my members to live with, while still getting away with it. 

With this in mind, new features were added like member blacklists (essentially lists of members to be berated and shunned by everyone else), role restoration (so the role war crimes committed against an individual would not vanish after they left), a message quoting system (to allow anyone to forever record any message into a database, to then be queried at random by individuals clueless of its sickening contents) or terrible "leisure" commands with absolutely no purpose (like an 8-ball command with no good answers, a file uploading service that never even worked because of the bot host's Internet firewall rules, the world's most complex "help" command, or even a myriad of vaguely-defined commands that were never implemented and would instead incite the user that executed it to annoy the bot owner). 

As this monstrosity grew, so did my disgust against it. With every update, endless lines of code were piled up on top of eachother without any semblance of order. The once streamlined class diagram would eventually turn into a mangled mess worthy of any favela electricity pole. My endurance would grow scarcer day after day, and after a grand total of 10 commits (this is not a joke, there were actually only 10 of those) I decided to abandon the project, and hope that for some miraculous reason the project gained enough sentience to delete itself in an attempt to improve Github as a whole.

The last time this project would see an update would be the 19th of February, 2021. That is, until today.

Today, I decided to finish what I had started. And by this I don't mean deleting the project - that would be a choice far too kind. Instead, I launched my code editor, cloned the private repository, and decided to do a little bit of cleanup in the hopes of making the codebase at least somewhat presentable. I have not made an effort to improve the bot, I have simply updated it to a state where it no longer breaks several articles in the Geneva Convention. Following this, I will publish this updated version onto a new Github repository, write some basic documentation (in the form of this README), and immediately make a public archive of it.

## I want to run this - how should I proceed?
You should not proceed, that's what you should do. I am currently working on a better bot that will superseed every single functionality this project once had. Ideally you would wait for that to be done, but if you are intent in letting this see the light of day, then I can but give you some advice.

- During the cleanup, I made a new `Statics` class found in the `util` package. You should give the static variables inside this class new values according to your needs.

- The JAR takes a single argument, which is your Discord bot's token.

For proper functionality, the bot requires a couple values to be stored inside the `DataManagementProcessor` class. These values are: 
- **channels.botspam**: the ID of the message channel where the bot will not have answer functionality. 
- **channels.logs**: the ID of the message channel where the bot will log miscellaneous information.
- **channels.archive**: the ID of the message channel where the bot will repost a blacklisted member's messages.
- **channels.general**: the ID of the message channel where the bot will send welcome/goodbye messages.

That's everything I remember from how you run this bot. Have fun(?)

## Fun facts
- The original repository was so cursed, Github thought it was written 65% in [Limbo](https://en.wikipedia.org/wiki/Limbo_(programming_language))
