# GitHub Release and Tag asset downloader

Downloads all assets for all releases and tags, including source archives.

Run like this:

`java -jar .\github-release-asset-downloader-1.0-SNAPSHOT-all.jar <USER> <REPO>`

of with a GitHub Access Token to reduce rate limiting:

`java -jar .\github-release-asset-downloader-1.0-SNAPSHOT-all.jar <USER> <REPO> <GITHUB_TOKEN>`