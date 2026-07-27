const STORAGE_TOKEN = "edu_tester_token";
const STORAGE_NAME = "edu_tester_name";

function showError(message) {
  const el = document.getElementById("error");
  if (!el) return;
  el.hidden = !message;
  el.textContent = message || "";
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.error || `Ошибка ${response.status}`);
  }
  return data;
}

async function bootRegisterPage() {
  try {
    const session = await api("/api/session");
    document.getElementById("test-title").textContent = session.title;
    document.getElementById("test-meta").textContent =
      `Вопросов: ${session.questionCount}`;
  } catch (e) {
    document.getElementById("test-title").textContent = "Сессия недоступна";
    document.getElementById("test-meta").textContent = e.message;
    document.getElementById("register-form").hidden = true;
    return;
  }

  document.getElementById("register-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    showError("");
    const form = event.target;
    const firstName = form.firstName.value.trim();
    const lastName = form.lastName.value.trim();
    const button = form.querySelector("button");
    button.disabled = true;
    try {
      const data = await api("/api/register", {
        method: "POST",
        body: JSON.stringify({ firstName, lastName }),
      });
      sessionStorage.setItem(STORAGE_TOKEN, data.token);
      sessionStorage.setItem(STORAGE_NAME, `${data.firstName} ${data.lastName}`);
      window.location.href = "/test";
    } catch (e) {
      showError(e.message);
      button.disabled = false;
    }
  });
}

async function bootTestPage() {
  const token = sessionStorage.getItem(STORAGE_TOKEN);
  const name = sessionStorage.getItem(STORAGE_NAME);
  if (!token) {
    window.location.href = "/";
    return;
  }

  document.getElementById("student-name").textContent = name || "";

  let questions = [];
  try {
    const session = await api("/api/session");
    document.getElementById("test-title").textContent = session.title;
    const payload = await api("/api/questions");
    questions = payload.questions || [];
  } catch (e) {
    showError(e.message);
    return;
  }

  const form = document.getElementById("test-form");
  form.innerHTML = "";
  questions.forEach((q, index) => {
    const block = document.createElement("section");
    block.className = "question";
    const title = document.createElement("h2");
    title.textContent = `${index + 1}. ${q.text}`;
    block.appendChild(title);
    q.options.forEach((option, optionIndex) => {
      const label = document.createElement("label");
      label.className = "option";
      const input = document.createElement("input");
      input.type = "radio";
      input.name = q.id;
      input.value = String(optionIndex);
      input.required = true;
      const span = document.createElement("span");
      span.textContent = option;
      label.appendChild(input);
      label.appendChild(span);
      block.appendChild(label);
    });
    form.appendChild(block);
  });

  document.getElementById("submit-btn").addEventListener("click", async () => {
    showError("");
    const answers = [];
    for (const q of questions) {
      const selected = form.querySelector(`input[name="${q.id}"]:checked`);
      if (!selected) {
        showError("Ответьте на все вопросы");
        return;
      }
      answers.push({ questionId: q.id, optionIndex: Number(selected.value) });
    }

    const button = document.getElementById("submit-btn");
    button.disabled = true;
    try {
      const result = await api("/api/submit", {
        method: "POST",
        body: JSON.stringify({ token, answers }),
      });
      document.querySelector(".card.wide").hidden = true;
      const resultCard = document.getElementById("result-card");
      resultCard.hidden = false;
      document.getElementById("result-text").textContent =
        `${result.firstName} ${result.lastName}: ${result.score} из ${result.maxScore}` +
        (result.alreadySubmitted ? " (уже было сдано)" : "");
      sessionStorage.removeItem(STORAGE_TOKEN);
    } catch (e) {
      showError(e.message);
      button.disabled = false;
    }
  });
}
