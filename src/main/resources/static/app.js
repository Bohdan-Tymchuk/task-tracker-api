const apiBase = '/api/tasks';
const tasksTable = document.getElementById('tasksTable');
const tasksBody = tasksTable ? tasksTable.querySelector('tbody') : null;
const tableWrap = document.getElementById('tableWrap');
const feedback = document.getElementById('feedback');
const statusLine = document.getElementById('statusLine');
const createForm = document.getElementById('createForm');
const filterForm = document.getElementById('filterForm');
const resetFilters = document.getElementById('resetFilters');

const statusOptions = ['PENDING', 'IN_PROGRESS', 'COMPLETED'];

function setFeedback(message, isError = false) {
  feedback.textContent = message || '';
  feedback.classList.toggle('error', Boolean(isError));
}

function setFieldError(name, message) {
  const el = document.querySelector(`[data-error-for="${name}"]`);
  if (el) {
    el.textContent = message || '';
    el.classList.toggle('error', Boolean(message));
  }
}

function clearFieldErrors() {
  document.querySelectorAll('[data-error-for]').forEach((el) => {
    el.textContent = '';
    el.classList.remove('error');
  });
}

function validateCreateForm(data) {
  let valid = true;
  clearFieldErrors();

  const title = data.get('title').trim();
  const description = data.get('description').trim();
  const dueDate = data.get('dueDate');

  if (!title) {
    setFieldError('title', 'Title is required.');
    valid = false;
  }
  if (!description) {
    setFieldError('description', 'Description is required.');
    valid = false;
  }
  if (!dueDate) {
    setFieldError('dueDate', 'Due date is required.');
    valid = false;
  } else {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const picked = new Date(dueDate);
    if (Number.isNaN(picked.getTime())) {
      setFieldError('dueDate', 'Due date is invalid.');
      valid = false;
    } else if (picked < today) {
      setFieldError('dueDate', 'Due date cannot be in the past.');
      valid = false;
    }
  }

  if (!valid) {
    setFeedback('Fix the highlighted fields.', true);
  }

  return valid;
}

async function fetchTasks(params = {}) {
  const url = new URL(apiBase, window.location.origin);
  Object.entries(params).forEach(([k, v]) => {
    if (v) url.searchParams.set(k, v);
  });
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error('Failed to fetch tasks');
  }
  return res.json();
}

async function createTask(payload) {
  const res = await fetch(apiBase, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const msg = await res.text();
    throw new Error(msg || 'Failed to create task');
  }
  return res.json();
}

async function updateStatus(id, status) {
  const res = await fetch(`${apiBase}/${id}/status`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });
  if (!res.ok) {
    const msg = await res.text();
    throw new Error(msg || 'Failed to update status');
  }
  return res.json();
}

async function deleteTask(id) {
  const res = await fetch(`${apiBase}/${id}`, { method: 'DELETE' });
  if (!res.ok) {
    throw new Error('Failed to delete task');
  }
}

function toDateString(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toISOString().slice(0, 10);
}

function renderTasks(tasks) {
  if (!tasksTable || !tasksBody) {
    return;
  }

  tasksBody.innerHTML = '';
  tasksTable.classList.remove('no-data');
  if (tableWrap) {
    tableWrap.style.display = 'block';
  }

  if (!tasks.length) {
    tasksTable.classList.add('no-data');
    tasksBody.innerHTML = '<tr class="empty-row"><td colspan="5">No tasks yet.</td></tr>';
    return;
  }

  tasks.forEach((task) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${task.title}</td>
      <td>${task.description}</td>
      <td>${toDateString(task.dueDate)}</td>
      <td><span class="status-tag" data-state="${task.status}">${task.status.replace('_', ' ')}</span></td>
      <td class="actions"></td>
    `;
    const actions = tr.querySelector('.actions');

    const select = document.createElement('select');
    statusOptions.forEach((opt) => {
      const option = document.createElement('option');
      option.value = opt;
      option.textContent = opt.replace('_', ' ');
      if (opt === task.status) option.selected = true;
      select.appendChild(option);
    });
    select.addEventListener('change', async (e) => {
      try {
        await updateStatus(task.id, e.target.value);
        setFeedback('Status updated.');
        loadTasks();
      } catch (err) {
        setFeedback(err.message, true);
        e.target.value = task.status;
      }
    });

    const del = document.createElement('button');
    del.textContent = 'Delete';
    del.classList.add('danger');
    del.addEventListener('click', async () => {
      if (!confirm('Delete this task?')) return;
      try {
        await deleteTask(task.id);
        setFeedback('Task deleted.');
        loadTasks();
      } catch (err) {
        setFeedback(err.message, true);
      }
    });

    actions.appendChild(select);
    actions.appendChild(del);
    tasksBody.appendChild(tr);
  });
}

async function loadTasks() {
  setFeedback('');
  if (!filterForm) {
    return;
  }
  const data = new FormData(filterForm);
  const filters = {
    status: data.get('status'),
    dueBefore: data.get('dueBefore'),
  };
  try {
    const tasks = await fetchTasks(filters);
    renderTasks(tasks);
  } catch (err) {
    setFeedback(err.message, true);
  }
}

if (createForm) {
  createForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = new FormData(createForm);
    if (!validateCreateForm(data)) return;

    const payload = {
      title: data.get('title').trim(),
      description: data.get('description').trim(),
      dueDate: data.get('dueDate'),
    };

    try {
      await createTask(payload);
      createForm.reset();
      clearFieldErrors();
      setFeedback('Task created.');
      loadTasks();
    } catch (err) {
      setFeedback(err.message, true);
    }
  });
}

if (filterForm) {
  filterForm.addEventListener('submit', (e) => {
    e.preventDefault();
    loadTasks();
  });
}

if (resetFilters) {
  resetFilters.addEventListener('click', () => {
    if (filterForm) {
      filterForm.reset();
    }
    loadTasks();
  });
}

function init() {
  if (statusLine) {
    statusLine.textContent = `API: ${apiBase}`;
  }
  loadTasks();
}

init();
