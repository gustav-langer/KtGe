package de.thecommcraft.ktge.docs

import kotlinx.html.*
import kotlinx.html.stream.createHTML



fun WebsiteDir.generateIndexPage(): Unit {
    return file("index.html") {
        html {
            lang = "en"
            head {
                title("KtGe")
                link {
                    rel = "preconnect"
                    href = "https://fonts.googleapis.com"
                }
                link {
                    rel = "preconnect"
                    attributes["crossorigin"] = "anonymous"
                    href = "https://fonts.gstatic.com"
                }
                link {
                    rel = "stylesheet"
                    href = "https://fonts.googleapis.com/css2?family=Montserrat:ital,wght@0,100..900;1,100..900&family=Protest+Strike&display=swap"
                }
                link {
                    rel = "stylesheet"
                    href = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/default.min.css"
                }
                link {
                    rel = "stylesheet"
                    href = "./style.css"
                }
                script {
                    src = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/highlight.min.js"
                }
                script {
                    src = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/languages/go.min.js"
                }
            }
            body {
                container {
                    header("KtGe")
                    subHeader("a small game engine written in Kotlin")
                    ordinaryParagraph("This page will probably have some documentation in the future, but that still needs to be written.")
                }
            }
        }
    }
}
/*
<html lang="en">
<head>
<title>KtGe</title>
<link href="https://fonts.googleapis.com" rel="preconnect">
<link crossorigin href="https://fonts.gstatic.com" rel="preconnect">
<link href="https://fonts.googleapis.com/css2?family=Montserrat:ital,wght@0,100..900;1,100..900&family=Protest+Strike&display=swap"
rel="stylesheet">
<link href="./style.css" rel="stylesheet">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/default.min.css">
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/highlight.min.js"></script>

<!-- and it's easy to individually load additional languages -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/languages/go.min.js"></script>
</head>
<body>
<div class="container">
<div class="header"><h1>KtGe</h1></div>
<div class="subheader"><h2>a small game engine written in Kotlin</h2></div>
<p>This page will probably have some documentation in the future, but that still needs to be written.</p>
</div>
</body>
</html>
*/