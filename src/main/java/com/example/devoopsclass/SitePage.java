package com.example.devoopsclass;

final class SitePage {
    private SitePage() {
    }

    static String render(String title, String activePath, String content, String additionalStyles) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <meta name="theme-color" content="#101827">
                    <title>%s</title>
                    <style>
                        :root {
                            color-scheme: dark;
                            font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                            background: #101827;
                            color: #f4f7ff;
                        }
                        * {
                            box-sizing: border-box;
                        }
                        body {
                            background:
                                radial-gradient(ellipse at 50%% -15%%, rgba(75, 122, 184, 0.38), transparent 55%%),
                                linear-gradient(145deg, #101827, #172642 58%%, #101827);
                            margin: 0;
                            min-height: 100vh;
                        }
                        .site-shell {
                            margin: 0 auto;
                            max-width: 1080px;
                            padding: 1.25rem 1.5rem 3rem;
                            width: 100%%;
                        }
                        .site-nav {
                            align-items: center;
                            border: 1px solid rgba(196, 210, 237, 0.12);
                            border-radius: 1rem;
                            background: rgba(12, 20, 34, 0.62);
                            display: flex;
                            flex-wrap: wrap;
                            gap: 1rem;
                            justify-content: space-between;
                            padding: 0.8rem 1rem;
                        }
                        .brand {
                            color: #f4f7ff;
                            font-size: 0.92rem;
                            font-weight: 750;
                            letter-spacing: 0.08em;
                            text-decoration: none;
                            text-transform: uppercase;
                        }
                        .brand span {
                            color: #68e0b0;
                        }
                        .nav-links {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 0.35rem;
                        }
                        .nav-links a {
                            border-radius: 0.7rem;
                            color: #c4d2ed;
                            font-size: 0.9rem;
                            padding: 0.55rem 0.75rem;
                            text-decoration: none;
                            transition: background 160ms ease, color 160ms ease;
                        }
                        .nav-links a:hover,
                        .nav-links a[aria-current="page"] {
                            background: rgba(104, 224, 176, 0.12);
                            color: #8af0c3;
                        }
                        .page-content {
                            margin: clamp(3rem, 10vh, 7rem) auto 0;
                            max-width: 760px;
                        }
                        .content-card {
                            background: linear-gradient(145deg, rgba(29, 45, 72, 0.92), rgba(19, 31, 51, 0.88));
                            border: 1px solid rgba(196, 210, 237, 0.14);
                            border-radius: 1.5rem;
                            box-shadow: 0 24px 80px rgba(0, 0, 0, 0.28);
                            padding: clamp(1.5rem, 5vw, 3.5rem);
                        }
                        .eyebrow {
                            color: #68e0b0;
                            font-size: 0.78rem;
                            font-weight: 750;
                            letter-spacing: 0.14em;
                            margin: 0 0 1rem;
                            text-transform: uppercase;
                        }
                        .content-card h1 {
                            font-size: clamp(2.1rem, 7vw, 3.7rem);
                            letter-spacing: -0.045em;
                            line-height: 1.08;
                            margin: 0;
                        }
                        .content-card p {
                            color: #c4d2ed;
                            font-size: clamp(1rem, 2.5vw, 1.15rem);
                            line-height: 1.75;
                            margin: 1.25rem 0 0;
                            max-width: 58ch;
                        }
                        .status-pill {
                            align-items: center;
                            border: 1px solid rgba(104, 224, 176, 0.45);
                            border-radius: 999px;
                            color: #8af0c3;
                            display: inline-flex;
                            font-size: 0.9rem;
                            gap: 0.55rem;
                            margin-top: 1.6rem;
                            padding: 0.65rem 1rem;
                        }
                        .status-pill::before {
                            background: #68e0b0;
                            border-radius: 50%%;
                            content: "";
                            height: 0.5rem;
                            width: 0.5rem;
                        }
                        .home-card {
                            text-align: center;
                        }
                        .home-card p {
                            margin-left: auto;
                            margin-right: auto;
                        }
                        %s
                        @media (max-width: 560px) {
                            .site-shell {
                                padding: 0.75rem 0.75rem 2rem;
                            }
                            .site-nav {
                                align-items: flex-start;
                                flex-direction: column;
                            }
                            .nav-links {
                                gap: 0.1rem;
                            }
                            .nav-links a {
                                font-size: 0.82rem;
                                padding: 0.5rem 0.58rem;
                            }
                        }
                        @media (prefers-reduced-motion: reduce) {
                            *,
                            *::before,
                            *::after {
                                scroll-behavior: auto !important;
                                transition-duration: 0.01ms !important;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="site-shell">
                        <nav class="site-nav" aria-label="Main navigation">
                            <a class="brand" href="/">Space <span>Hyper</span></a>
                            <div class="nav-links">
                                %s
                            </div>
                        </nav>
                        <main class="page-content">%s</main>
                    </div>
                </body>
                </html>
                """.formatted(title, additionalStyles, navigation(activePath), content);
    }

    private static String navigation(String activePath) {
        return link("/", "Home", activePath)
                + link("/about", "About", activePath)
                + link("/contact", "Contact", activePath)
                + link("/deploy", "Deploy", activePath)
                + link("/binaya", "Binaya", activePath);
    }

    private static String link(String path, String label, String activePath) {
        String current = path.equals(activePath) ? " aria-current=\"page\"" : "";
        return "<a href=\"" + path + "\"" + current + ">" + label + "</a>";
    }
}
