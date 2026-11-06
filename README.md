# NoteBlockTool (Web)
Web App for rendering Minecraft Note Block Studio songs to audio files.

To get started, visit the [official page](https://raphimc.net/noteblocktool).  
For a more detailed guide, check out the [Usage](#usage) section below.

## Features
- Listen to or export Note Block Studio and Note Block World songs
- Fully clientside (Everything runs in your browser)
- Very accurate and high quality song renderer
    - Supports Custom Instruments
    - Supports Tempo Changers
    - Supports Sound Stoppers
    - Optional timing jitter for a more natural sound
- Supports exporting to all common audio formats (WAV, FLAC, MP3, OPUS)
- Adds metadata to the exported audio files (title, author, description)
- Very fast and easy to use

## Usage
### Loading a song
After opening the web app, you can load a song by clicking the "Choose file" button,
dragging and dropping a song file into the table,
or by entering a NoteBlockWorld song URL into the input field and clicking the "Load song" button.

### Loading custom sounds (Optional)
After loading a song, you can see the instrument list and optionally load custom sounds.
NoteBlockTool will automatically download vanilla Minecraft sounds from the official Mojang servers.
If you want to use your own custom sounds, click the "Choose folder" button in the "Custom Sounds" section and select the folder containing your custom sounds.

### Rendering the song
To render the song to an audio file, click the "Render" button. This will open the render dialog where you can configure the audio format, playback and rendering options.
The default options usually work great as they are, but you can change them if you want to customize the output.
After configuring the options, click the "Render" button in the dialog to start rendering the song.

### Downloading the audio file
After the rendering is complete, a green box will appear at the bottom of the dialog with a preview of the rendered audio file and a download button.
Optionally there can also be warning messages if some instrument sounds are missing.

## Development
The backend of NoteBlockTool is written in Java and the frontend is written in TypeScript and Svelte.

### Building the backend
To build the backend, run the following command in the root directory:
```bash
./gradlew build
```
Optionally append `-Penvironment=prod` to create an optimized build.

### Building the frontend
To start a development server, run the following command in the `frontend` directory:
```bash
npm run dev
```
To build the frontend, run the following command in the `frontend` directory:
```bash
npm run build
```

## AI disclosure
NoteBlockTool's frontend is being developed with extensive AI assistance. The generated code is manually inspected and adjusted where necessary.

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/RaphiMC/NoteBlockTool-Web/issues).  
If you just want to talk or need help using NoteBlockTool feel free to join my
[Discord](https://raphimc.net/discord).
