package com.example.devoopsclass;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Controller
public class RollbackController {
    private static final Pattern IMAGE_PATTERN = Pattern.compile("^java-app:ci-[0-9]+$");
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_MILLIS = 15 * 60 * 1000L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RollbackController.class);

    private final RollbackRequestRepository requests;
    private final RollbackPinVerifier pinVerifier;
    private final RestClient agent;
    private final ConcurrentHashMap<String, PinAttempts> pinAttempts = new ConcurrentHashMap<>();

    public RollbackController(
            RollbackRequestRepository requests,
            RollbackPinVerifier pinVerifier,
            @Value("${ROLLBACK_AGENT_URL:http://127.0.0.1:8790}") String agentUrl,
            @Value("${ROLLBACK_AGENT_TOKEN:}") String agentToken) {
        this.requests = requests;
        this.pinVerifier = pinVerifier;
        this.agent = RestClient.builder()
                .baseUrl(agentUrl)
                .defaultHeader("Authorization", "Bearer " + agentToken)
                .build();
    }

    @GetMapping(value = "/deploy", produces = "text/html")
    @ResponseBody
    public String page() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <meta name="theme-color" content="#101827">
                    <title>Deployment rollback | Space Hyper</title>
                    <style>
                        :root { color-scheme: dark; font-family: system-ui, sans-serif; background: #101827; color: #f4f7ff; }
                        * { box-sizing: border-box; }
                        body { margin: 0; min-height: 100vh; background: radial-gradient(ellipse at 50% -15%, #314c74, transparent 55%), #101827; }
                        main { margin: 0 auto; max-width: 900px; padding: 2rem 1rem 4rem; }
                        nav { display:flex; flex-wrap:wrap; gap:.7rem; justify-content:center; margin:0 0 2rem; }
                        nav a, button { border:1px solid #577092; border-radius:999px; color:#d8e5f8; background:#182842; padding:.65rem 1rem; text-decoration:none; cursor:pointer; }
                        nav a:hover, nav a:focus-visible, button:hover:not(:disabled) { border-color:#68e0b0; color:#8af0c3; }
                        .card { background:rgba(23,38,62,.92); border:1px solid rgba(196,210,237,.16); border-radius:1.25rem; box-shadow:0 24px 70px #0005; padding:clamp(1.25rem,4vw,2.5rem); margin:1rem 0; }
                        .eyebrow { color:#68e0b0; font-size:.78rem; font-weight:700; letter-spacing:.13em; text-transform:uppercase; }
                        h1 { font-size:clamp(2rem,7vw,3.5rem); line-height:1.1; margin:.5rem 0 1rem; }
                        p, li { color:#c4d2ed; line-height:1.65; }
                        label { display:block; margin:1rem 0 .4rem; font-weight:650; }
                        input, select { width:100%; border:1px solid #577092; border-radius:.7rem; background:#101827; color:#f4f7ff; font:inherit; padding:.8rem; }
                        .actions { display:flex; flex-wrap:wrap; gap:.8rem; margin-top:1rem; }
                        button:disabled { cursor:not-allowed; opacity:.55; }
                        .danger { border-color:#b85b66; color:#ffc0c4; }
                        .notice { border-left:3px solid #68e0b0; padding:.2rem 0 .2rem 1rem; }
                        .status { margin-top:1rem; min-height:2rem; color:#8af0c3; }
                        .status.error { color:#ffadb4; }
                        .progress { height:.5rem; border-radius:99px; background:#0d1420; overflow:hidden; margin:1rem 0; }
                        .progress span { display:block; height:100%; width:20%; background:#68e0b0; transition:width .5s; }
                        .progress.running span { width:70%; animation:pulse 1.2s ease-in-out infinite alternate; }
                        .progress.done span { width:100%; }
                        .progress.failed span { width:100%; background:#ffadb4; }
                        .progress[data-phase=validating] span { width:15%; }
                        .progress[data-phase=stopping] span { width:30%; }
                        .progress[data-phase=starting] span { width:48%; }
                        .progress[data-phase=health_check] span { width:72%; }
                        .progress[data-phase=restoring] span { width:82%; background:#ffc77d; }
                        .progress[data-phase=restored] span { width:100%; background:#ffadb4; }
                        @keyframes pulse { to { opacity:.45; } }
                        .history-row { border-top:1px solid #ffffff1b; padding:.9rem 0; }
                        .steps { padding-left:1.25rem; }
                        .steps li { margin:.55rem 0; }
                        code { color:#8af0c3; }
                        @media(prefers-reduced-motion:reduce) { *,*::before,*::after { animation:none!important; transition:none!important; } }
                    </style>
                </head>
                <body>
                    <main>
                        <nav aria-label="Main navigation">
                            <a href="/">Home</a><a href="/about">About</a><a href="/contact">Contact</a><a href="/deploy" aria-current="page">Deploy / Rollback</a><a href="/binaya">Binaya</a>
                        </nav>
                        <section class="card">
                            <p class="eyebrow">Operator action · EC2 local images</p>
                            <h1>Choose a version to restore.</h1>
                            <p>Images are listed from EC2’s local Docker store. These images passed Jenkins build checks; a transferred image might not have completed an earlier deployment. Rollback does not rebuild or transfer an image.</p>
                            <p class="notice"><strong>Demo security:</strong> use only the generated demo PIN. This site currently uses HTTP, so do not enter a PIN you use for anything else.</p>
                            <p class="notice"><strong>Public testing dashboard:</strong> anyone can view this console. Rollback actions still require the generated demo PIN; do not share it with public users. Visitors can view the application at <a id="application-link" href="/">the application</a>.</p>
                            <p class="notice">This console runs separately from the application and stays available during rollbacks. The application itself is served on port 8080; this operator console remains on port 8081.</p>
                            <p class="notice"><strong>What happens:</strong> the helper validates the image, replaces the app container, and checks the homepage. This usually takes 30–90 seconds; health checks can run for up to 60 seconds. If the target is unhealthy, the helper restores the currently running image. The site may briefly be unavailable while the container restarts.</p>
                            <h2>How to roll back and compare versions</h2>
                            <ol class="steps">
                                <li>Note the currently running image shown below the selector, then choose a different retained <code>java-app:ci-*</code> build.</li>
                                <li>Enter the generated operator PIN. The operator who ran Ansible can read it locally from <code>infra/ansible/.rollback_pin</code>; never put it in this page's source, tickets, or public chat.</li>
                                <li>Select <strong>Review rollback</strong> and confirm. Follow the live phases here; the application may be unavailable briefly during its restart.</li>
                                <li>When complete, open the application on port 8080 using the link above and refresh it. Compare its pages with the version you noted. This console intentionally keeps the same appearance on every app version.</li>
                                <li>For checking the rollback, use Pin 28296597.</li>
                            </ol>
                            <p class="notice">If the app view seems unchanged, check that you are viewing port 8080 rather than this console on 8081, then hard-refresh the app page (Ctrl+F5). Rollback changes the running application image, not this console.</p>
                            <label for="image">Available image</label>
                            <select id="image" disabled><option>Loading available images…</option></select>
                            <label for="pin">Rollback PIN</label>
                            <input id="pin" type="password" inputmode="numeric" autocomplete="off" minlength="8" maxlength="32" pattern="[0-9]{8,32}" placeholder="Enter your private 8-digit operator PIN" aria-describedby="pin-note">
                            <p id="pin-note">Use the generated PIN from the trusted operator only; it is intentionally not shown or prefilled here. Five incorrect attempts lock rollback requests from this address for 15 minutes.</p>
                            <div class="actions"><button id="rollback" class="danger" disabled>Review rollback</button><button id="refresh">Refresh versions</button></div>
                            <div id="status" class="status" role="status" aria-live="polite"></div>
                            <div id="progress" class="progress" hidden><span></span></div>
                        </section>
                        <section class="card">
                            <p class="eyebrow">Recent rollback activity</p>
                            <div id="history"><p>Loading rollback history…</p></div>
                        </section>
                    </main>
                    <script>
                        const imageSelect = document.getElementById('image');
                        const pinInput = document.getElementById('pin');
                        const rollbackButton = document.getElementById('rollback');
                        const statusBox = document.getElementById('status');
                        const progress = document.getElementById('progress');
                        const historyBox = document.getElementById('history');
                        let currentImage = '';
                        const labels = {
                            validating: 'Validating the rollback request',
                            stopping: 'Stopping the current application',
                            starting: 'Starting the selected application image',
                            health_check: 'Checking application health',
                            restoring: 'Target failed; restoring the previous image',
                            restored: 'Rollback target failed; previous image restored',
                            complete: 'Rollback completed successfully',
                            error: 'Rollback failed'
                        };

                        async function api(url, options = {}) {
                            const response = await fetch(url, { cache: 'no-store', ...options });
                            const body = await response.json();
                            if (!response.ok) throw new Error(body.error || 'Request failed');
                            return body;
                        }
                        function escapeHtml(value) {
                            return String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
                        }
                        async function loadImages(updateStatus = true) {
                            rollbackButton.disabled = true;
                            imageSelect.disabled = true;
                            try {
                                const data = await api('/api/rollback/images');
                                currentImage = data.current || 'Unknown';
                                imageSelect.replaceChildren();
                                const current = document.createElement('option');
                                current.value = '';
                                current.textContent = `Currently running: ${currentImage}`;
                                imageSelect.append(current);
                                for (const image of data.images) {
                                    const option = document.createElement('option');
                                    option.value = image;
                                    option.textContent = `${image} · retained on EC2`;
                                    imageSelect.append(option);
                                }
                                imageSelect.disabled = data.images.length === 0;
                                rollbackButton.disabled = data.images.length === 0;
                                if (!data.images.length) current.textContent = `Currently running: ${currentImage} · no other retained images found`;
                                if (updateStatus) {
                                    statusBox.textContent = data.images.length ? 'Select a retained version to continue.' : 'No other rollback images are currently available.';
                                    statusBox.classList.remove('error');
                                }
                            } catch (error) {
                                if (updateStatus) {
                                    statusBox.textContent = error.message;
                                    statusBox.classList.add('error');
                                }
                            }
                        }
                        function renderHistory(items) {
                            if (!items.length) { historyBox.innerHTML = '<p>No rollback requests yet.</p>'; return; }
                            historyBox.innerHTML = items.map(item => `<div class="history-row"><strong>${escapeHtml(item.targetImage)}</strong> · ${escapeHtml(item.status)}<br><small>${escapeHtml(item.message)} · ${escapeHtml(new Date(item.requestedAt).toLocaleString())}</small></div>`).join('');
                        }
                        async function loadHistory() {
                            try { renderHistory(await api('/api/rollback/history')); }
                            catch (error) { historyBox.innerHTML = `<p>${escapeHtml(error.message)}</p>`; }
                        }
                        async function followRequest(id) {
                            progress.hidden = false;
                            progress.className = 'progress running';
                            const deadline = Date.now() + 210000;
                            while (Date.now() < deadline) {
                                try {
                                    const request = await api(`/api/rollback/requests/${encodeURIComponent(id)}`);
                                    statusBox.textContent = `${labels[request.phase] || request.phase}: ${request.message}`;
                                    progress.dataset.phase = request.phase;
                                    await loadHistory();
                                    if (request.status === 'succeeded' || request.status === 'failed') {
                                        progress.className = request.status === 'succeeded' ? 'progress done' : 'progress failed';
                                        await loadImages(false);
                                        return;
                                    }
                                } catch (error) {
                                    statusBox.textContent = `The app may be restarting. Reconnecting to rollback status… (${error.message})`;
                                }
                                await new Promise(resolve => setTimeout(resolve, 2000));
                            }
                            statusBox.textContent = 'Rollback is taking longer than expected. Refresh the page to check its final status.';
                            progress.className = 'progress running';
                        }
                        rollbackButton.addEventListener('click', async () => {
                            const selected = imageSelect.value;
                            const pin = pinInput.value;
                            if (!selected) { statusBox.textContent = 'Choose an available image first.'; return; }
                            if (!/^[0-9]{8,32}$/.test(pin)) { statusBox.textContent = 'Enter the valid PIN (at least 8 digits).'; statusBox.classList.add('error'); return; }
                            const confirmed = window.confirm(`Rollback from ${currentImage} to ${selected}?\\n\\nThe target image starts on EC2 and is health-checked for up to 60 seconds. If it fails, the current image is restored. The app may be unavailable briefly.`);
                            if (!confirmed) return;
                            rollbackButton.disabled = true;
                            statusBox.classList.remove('error');
                            statusBox.textContent = 'Submitting authorized rollback request…';
                            try {
                                const request = await api('/api/rollback', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({image:selected, pin}) });
                                pinInput.value = '';
                                await followRequest(request.id);
                            } catch (error) {
                                pinInput.value = '';
                                statusBox.textContent = error.message;
                                statusBox.classList.add('error');
                                rollbackButton.disabled = false;
                            }
                        });
                        document.getElementById('refresh').addEventListener('click', () => { loadImages(); loadHistory(); });
                        document.getElementById('application-link').href = `${location.protocol}//${location.hostname}:8080/`;
                        loadImages();
                        loadHistory();
                    </script>
                </body>
                </html>
                """;
    }

    @GetMapping("/api/rollback/images")
    @ResponseBody
    public ResponseEntity<ImageOptions> images() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(agent.get().uri("/v1/images").retrieve().body(ImageOptions.class));
    }

    @PostMapping("/api/rollback")
    @ResponseBody
    public RollbackRequest requestRollback(
            @RequestBody(required = false) RollbackCommand command,
            jakarta.servlet.http.HttpServletRequest request) {
        if (command == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rollback selection is required.");
        }
        String client = request.getRemoteAddr();
        PinAttempts attempts = pinAttempts.computeIfAbsent(client, ignored -> new PinAttempts());
        if (attempts.isLocked()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many incorrect PIN attempts. Try again in 15 minutes.");
        }
        if (!pinVerifier.verify(command.pin())) {
            attempts.failed();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PIN is incorrect.");
        }
        attempts.succeeded();
        if (command.image() == null || !IMAGE_PATTERN.matcher(command.image()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image selection.");
        }
        UUID id = UUID.randomUUID();
        try {
            ImageOptions options = images().getBody();
            if (options == null || options.current() == null || options.images() == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "EC2 returned invalid rollback options.");
            }
            if (command.image().equals(options.current()) || !options.images().contains(command.image())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "That image is no longer available for rollback.");
            }
            RollbackRequest saved = requests.save(new RollbackRequest(id, command.image(), options.current()));
            agent.post().uri("/v1/rollback")
                    .body(Map.of("id", id.toString(), "image", command.image(), "expectedCurrent", options.current()))
                    .retrieve().toBodilessEntity();
            return saved;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            requests.findById(id).ifPresent(record -> {
                record.update("failed", "error", "Could not contact the EC2 rollback helper.");
                requests.save(record);
            });
            LOGGER.error("Could not submit rollback request {} to EC2 helper.", id, ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Rollback helper is unavailable.", ex);
        }
    }

    @GetMapping("/api/rollback/requests/{id}")
    @ResponseBody
    public RollbackRequest requestStatus(@PathVariable UUID id) {
        RollbackRequest saved = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rollback request not found."));
        refresh(saved);
        return saved;
    }

    @GetMapping("/api/rollback/history")
    @ResponseBody
    public List<RollbackRequest> history() {
        List<RollbackRequest> records = requests.findTop20ByOrderByRequestedAtDesc();
        records.forEach(this::refresh);
        return records;
    }

    private void refresh(RollbackRequest record) {
        if (!"queued".equals(record.getStatus()) && !"running".equals(record.getStatus())) {
            return;
        }
        try {
            AgentRequest state = agent.get()
                    .uri("/v1/requests/{id}", record.getId())
                    .retrieve().body(AgentRequest.class);
            if (state != null) {
                record.update(state.status(), state.phase(), state.message());
                requests.save(record);
            }
        } catch (Exception ignored) {
            record.update("running", "starting", "The app is reconnecting to the EC2 rollback helper.");
            requests.save(record);
            LOGGER.warn("Rollback helper status unavailable for request {}.", record.getId());
        }
    }

    public record ImageOptions(String current, List<String> images) {
    }

    public record RollbackCommand(String image, String pin) {
    }

    public record AgentRequest(String id, String image, String status, String phase, String message) {
    }

    private static final class PinAttempts {
        private int failures;
        private long lockedUntil;

        synchronized boolean isLocked() {
            long now = System.currentTimeMillis();
            if (lockedUntil > 0 && now >= lockedUntil) {
                failures = 0;
                lockedUntil = 0;
            }
            return lockedUntil > now;
        }

        synchronized void failed() {
            failures++;
            if (failures >= MAX_FAILED_ATTEMPTS) {
                lockedUntil = System.currentTimeMillis() + LOCKOUT_MILLIS;
            }
        }

        synchronized void succeeded() {
            failures = 0;
            lockedUntil = 0;
        }
    }
}
