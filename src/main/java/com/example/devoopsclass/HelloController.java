package com.example.devoopsclass;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping(value = "/", produces = "text/html")
    public String index() {
        String content = """
                <section class="content-card home-card">
                    <p class="eyebrow">Java · Docker · AWS · Jenkins</p>
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
                    <span class="status-pill">Deployed automatically with Jenkins CI/CD</span>
                </section>
                """;
        String styles = """
                .home-card h1 {
                    margin-inline: auto;
                    width: 100%;
                }
                .name-rotator {
                    display: grid;
                    grid-template-columns: 1fr;
                    margin-inline: auto;
                    min-height: 1.25em;
                    perspective: 700px;
                    width: 11ch;
                }
                .name-rotator > span {
                    grid-area: 1 / 1;
                    justify-self: center;
                    text-align: center;
                    white-space: pre;
                }
                .letter {
                    animation-delay: calc(var(--i) * 35ms);
                    animation-duration: 7s;
                    animation-iteration-count: infinite;
                    animation-timing-function: ease-in-out;
                    display: inline-block;
                    transform-style: preserve-3d;
                }
                .letter--space {
                    animation-name: space-letter;
                }
                .letter--amar {
                    animation-name: amar-letter;
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
                @media (prefers-reduced-motion: reduce) {
                    .letter {
                        animation: none;
                    }
                    .letter--amar {
                        display: none;
                    }
                }
                """;
        return SitePage.render("Space Hyper CI/CD Demo", "/", content, styles);
    }
}