# Spotify to youtube music playlist converter.
Convert spotify playlists to youtube music playlists.

Requires creating an app on https://developer.spotify.com/ and inputing the client secret and client id.
To sign in to youtube music hit the button which will open a webview and take the Cookie used to authenticate.

All login info is stored using EncryptedSharedPreferences.

# Setup
-------

1. Generate a new app at https://developer.spotify.com/dashboard
2. Run

.. code-block::

    spotify_to_ytmusic setup

For backwards compatibility you can also create your own file and pass it using ``--file settings.ini``.

If you want to transfer private playlists from Spotify (i.e. liked songs), choose "yes" for oAuth authentication, otherwise choose "no".
For oAuth authentication you should set ``http://localhost`` as redirect URI for your app in Spotify's developer dashboard.


<img src="https://github.com/user-attachments/assets/216945fb-0364-4a9c-a5e4-86b7eaf3ff3f" width="360" height="780">
<img src="https://github.com/user-attachments/assets/98a3427c-061d-4ea3-b64e-d0d812cd095d" width="360" height="780">
<img src="https://github.com/user-attachments/assets/f9f1afc4-98e7-44d3-bad6-9d646dbf83f0" width="360" height="780">
<img src="https://github.com/user-attachments/assets/6f8a85c9-111c-417e-b463-2b1f9d2f81e9" width="360" height="780">

