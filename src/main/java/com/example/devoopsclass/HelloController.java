package com.example.devoopsclass;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping(value = "/", produces = "text/html")
    public String index() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Space Hyper CI/CD Demo</title>
                    <style>
                        body {
                            align-items: center;
                            background: linear-gradient(135deg, #101827, #243b65);
                            color: #f4f7ff;
                            display: flex;
                            font-family: system-ui, sans-serif;
                            justify-content: center;
                            margin: 0;
                            min-height: 100vh;
                            text-align: center;
                        }
                        main {
                            padding: 2rem;
                        }
                        h1 {
                            font-size: clamp(2rem, 7vw, 4rem);
                            margin-bottom: 0.75rem;
                        }
                        .name-rotator {
                            display: inline-grid;
                            min-height: 1.25em;
                            perspective: 700px;
                            vertical-align: top;
                        }
                        .name-rotator > span {
                            grid-area: 1 / 1;
                            white-space: pre;
                        }
                        .letter {
                            display: inline-block;
                            transform-style: preserve-3d;
                            animation-delay: calc(var(--i) * 35ms);
                            animation-duration: 7s;
                            animation-iteration-count: infinite;
                            animation-timing-function: ease-in-out;
                        }
                        .visually-hidden {
                            clip: rect(0, 0, 0, 0);
                            clip-path: inset(50%);
                            height: 1px;
                            overflow: hidden;
                            position: absolute;
                            white-space: nowrap;
                            width: 1px;
                        }
                        .letter--space {
                            animation-name: space-letter;
                        }
                        .letter--amar {
                            animation-name: amar-letter;
                        }
                        @keyframes space-letter {
                            0%, 36% {
                                opacity: 1;
                                transform: rotateX(0) translateY(0);
                            }
                            44%, 88% {
                                opacity: 0;
                                transform: rotateX(90deg) translateY(-0.6em);
                            }
                            96%, 100% {
                                opacity: 1;
                                transform: rotateX(0) translateY(0);
                            }
                        }
                        @keyframes amar-letter {
                            0%, 38% {
                                opacity: 0;
                                transform: rotateX(-90deg) translateY(0.6em);
                            }
                            48%, 84% {
                                opacity: 1;
                                transform: rotateX(0) translateY(0);
                            }
                            94%, 100% {
                                opacity: 0;
                                transform: rotateX(90deg) translateY(-0.6em);
                            }
                        }
                        p {
                            color: #c4d2ed;
                            font-size: 1.2rem;
                        }
                        .badge {
                            border: 1px solid #68e0b0;
                            border-radius: 999px;
                            color: #68e0b0;
                            display: inline-block;
                            margin-top: 1rem;
                            padding: 0.5rem 1rem;
                        }
                        @media (prefers-reduced-motion: reduce) {
                            .letter {
                                animation: none;
                            }
                            .letter--amar {
                                display: none;
                            }
                        }
                    </style>
                </head>
                <body>
                    <main>
                        <h1 aria-label="Space Hyper and Amar Rana">
                            <span class="visually-hidden">Space Hyper and Amar Rana</span>
                            <span class="name-rotator" aria-hidden="true">
                                <span>
                                    <span class="letter letter--space" style="--i: 0">S</span><span class="letter letter--space" style="--i: 1">P</span><span class="letter letter--space" style="--i: 2">A</span><span class="letter letter--space" style="--i: 3">C</span><span class="letter letter--space" style="--i: 4">E</span><span class="letter letter--space" style="--i: 5"> </span><span class="letter letter--space" style="--i: 6">H</span><span class="letter letter--space" style="--i: 7">Y</span><span class="letter letter--space" style="--i: 8">P</span><span class="letter letter--space" style="--i: 9">E</span><span class="letter letter--space" style="--i: 10">R</span>
                                </span>
                                <span>
                                    <span class="letter letter--amar" style="--i: 0">A</span><span class="letter letter--amar" style="--i: 1">M</span><span class="letter letter--amar" style="--i: 2">A</span><span class="letter letter--amar" style="--i: 3">R</span><span class="letter letter--amar" style="--i: 4"> </span><span class="letter letter--amar" style="--i: 5">R</span><span class="letter letter--amar" style="--i: 6">A</span><span class="letter letter--amar" style="--i: 7">N</span><span class="letter letter--amar" style="--i: 8">A</span>
                                </span>
                            </span>
                        </h1>
                        <p>Java application is running on AWS EC2.</p>
                        <span class="badge">Deployed automatically with Jenkins CI/CD</span>
                    </main>
                </body>
                </html>
                """;
    }
}