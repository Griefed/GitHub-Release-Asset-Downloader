package de.griefed

fun main(args: Array<String>) {
    if (args.size == 3) {
        GitHubRepo.downloadAssets(args[0], args[1], args[2])
    } else {
        GitHubRepo.downloadAssets(args[0], args[1])
    }
}

