const $ = (selector) => document.querySelector(selector);
function readStore(key, fallback) { try { return JSON.parse(localStorage.getItem(key)) || fallback; } catch { return fallback; } }
const accounts = new Map(readStore('faceconnect_accounts', [['gagan', { password: 'gagan', name: 'Gagan' }]]));
function saveAccounts() { localStorage.setItem('faceconnect_accounts', JSON.stringify([...accounts])); }
let activeUser = { name: 'Gagan', initial: 'G' };
let activeCommentPost = null;
let draftMedia = null;
const notifications = [
  'Bob Jones accepted your friend request.',
  'Carol Ray commented on your post.',
  'Your memory from one year ago is ready to view.'
];
const messages = [{ from: 'Bob Jones', text: 'Hey Gagan! Are we still meeting today?', mine: false }, { from: 'You', text: 'Yes, see you at 5!', mine: true }];
const people = [{ name: 'Bob Jones', initial: 'B', color: 'green', mutual: 12 }, { name: 'Carol Ray', initial: 'C', color: 'purple', mutual: 8 }, { name: 'David Kim', initial: 'D', color: 'orange', mutual: 4 }];
const defaultPosts = [
  { name: 'Bob Jones', initial: 'B', color: 'green', time: '2 hrs', text: 'A beautiful day to learn something new. What are you working on today?', likes: 14, comments: ['Carol: Love this!', 'Gagan: Great thought, Bob.'], image: '☀️' },
  { name: 'Carol Ray', initial: 'C', color: 'purple', time: 'Yesterday', text: 'Small steps every day turn into big progress. Keep going!', likes: 28, comments: ['Bob: Needed this reminder.'], image: '' }
];
let posts = readStore('faceconnect_posts', defaultPosts);
function savePosts() { try { localStorage.setItem('faceconnect_posts', JSON.stringify(posts)); } catch { toast('Post is live for this session, but the media file is too large to save after refresh.'); } }

function escapeHtml(value) { const element = document.createElement('div'); element.textContent = value; return element.innerHTML; }
function renderPosts() {
  savePosts();
  $('#feedPosts').innerHTML = posts.map((post, index) => `<article class="post" data-post="${index}">
    <div class="post-head"><span class="avatar ${post.color}">${post.initial}</span><div><b>${escapeHtml(post.name)}</b><small>${post.time} · 🌐</small></div></div>
    <div class="post-text">${escapeHtml(post.text)}</div>${post.media ? `<div class="post-media">${post.media.type === 'video' ? `<video controls src="${escapeHtml(post.media.src)}"></video>` : `<img src="${escapeHtml(post.media.src)}" alt="Post upload" />`}</div>` : post.image ? `<div class="post-image">${post.image}</div>` : ''}
    <div class="post-stats">👍 ${post.likes} &nbsp; · &nbsp; ${post.comments.length} comments</div>
    <div class="post-buttons"><button data-like="${index}" class="${post.liked ? 'liked' : ''}">👍 Like</button><button data-comment="${index}">💬 Comment</button><button data-share="${index}">↗ Share</button></div>
  </article>`).join('');
  document.querySelectorAll('[data-like]').forEach(button => button.onclick = () => { const post = posts[button.dataset.like]; post.liked = !post.liked; post.likes += post.liked ? 1 : -1; renderPosts(); });
  document.querySelectorAll('[data-comment]').forEach(button => button.onclick = () => openComments(Number(button.dataset.comment)));
  document.querySelectorAll('[data-share]').forEach(button => button.onclick = () => { const post = posts[button.dataset.share]; posts.unshift({ ...post, name: activeUser.name, initial: activeUser.initial, color: 'blue', time: 'Just now', text: `Shared ${post.name}'s post: ${post.text}`, likes: 0, comments: [], image: post.image }); renderPosts(); toast('Post shared to your timeline.'); });
}
function openComments(index) { activeCommentPost = index; renderComments(); openModal('#commentModal'); }
function renderComments() { const post = posts[activeCommentPost]; $('#commentList').innerHTML = post.comments.length ? post.comments.map(c => `<div class="comment">${escapeHtml(c)}</div>`).join('') : '<p class="demo-hint">No comments yet. Start the conversation.</p>'; }
function openModal(id) { $(id).classList.remove('hidden'); }
function closeModals() { document.querySelectorAll('.modal').forEach(m => m.classList.add('hidden')); }
function closePanels() { document.querySelectorAll('.floating-panel,.search-panel').forEach(p => p.classList.add('hidden')); }
function toast(text) { const box = $('#toast'); box.textContent = text; box.classList.remove('hidden'); setTimeout(() => box.classList.add('hidden'), 2600); }
function setHome() { $('#secondaryView').classList.add('hidden'); $('#feedPosts').classList.remove('hidden'); document.querySelectorAll('.stories,.composer').forEach(e => e.classList.remove('hidden')); document.querySelectorAll('nav button').forEach(b => b.classList.remove('nav-active')); $('#navHome').classList.add('nav-active'); }
function showProfile() {
  closePanels();
  document.querySelectorAll('.stories,.composer,#feedPosts').forEach(e => e.classList.add('hidden'));
  const panel = $('#secondaryView');
  panel.classList.remove('hidden'); panel.classList.add('profile-page');
  const ownPosts = posts.filter(p => p.name === activeUser.name);
  panel.innerHTML = `<section class="profile-cover"></section>
    <section class="profile-identity"><div class="profile-avatar">${activeUser.initial}</div><div class="profile-name"><h1>${escapeHtml(activeUser.name)}</h1><p>124 friends</p></div><div class="profile-actions"><button id="addStoryProfile">＋ Add to story</button><button id="editProfile">✎ Edit profile</button></div></section>
    <section class="tabbar"><button id="profilePosts">Posts</button><button id="profileAbout">About</button><button id="profileFriends">Friends</button><button id="profilePhotos">Photos</button></section>
    <section class="profile-content"><aside><article class="profile-panel"><h2>Intro</h2><p>✦ Learning, building and sharing.</p><p>⌂ Lives in India</p><p>♟ Followed by 124 people</p><button class="primary" id="introEdit">Edit details</button></article><article class="profile-panel"><h2>Friends</h2><p>124 friends</p><div class="profile-friends">${people.map(p => `<div><span class="avatar ${p.color}">${p.initial}</span><br>${p.name}</div>`).join('')}</div></article></aside><main><article class="profile-panel"><button id="profileCompose">What's on your mind, ${activeUser.name.split(' ')[0]}?</button></article>${ownPosts.length ? ownPosts.map(p => `<article class="profile-panel profile-post"><b>${escapeHtml(p.name)}</b><p>${escapeHtml(p.text)}</p><small>👍 ${p.likes} · ${p.comments.length} comments</small></article>`).join('') : '<article class="profile-panel"><b>Welcome to your profile</b><p>Create a post and it will appear here.</p></article>'}</main></section>`;
  document.querySelectorAll('#profileCompose,#addStoryProfile').forEach(b => b.onclick = () => openModal('#postModal'));
  $('#editProfile').onclick = () => toast('Profile editor is ready for backend saving.');
  $('#introEdit').onclick = () => toast('Intro settings opened.');
  ['profilePosts','profileAbout','profileFriends','profilePhotos'].forEach(id => $(`#${id}`).onclick = () => toast(`${$(`#${id}`).textContent} tab selected.`));
  document.querySelectorAll('nav button').forEach(b => b.classList.remove('nav-active'));
}
function showView(view) {
  closePanels(); $('#feedPosts').classList.add('hidden'); document.querySelectorAll('.stories,.composer').forEach(e => e.classList.add('hidden')); const panel = $('#secondaryView'); panel.classList.remove('hidden'); panel.classList.remove('profile-page');
  const title = { friends:'Friends', watch:'Watch', marketplace:'Marketplace', groups:'Groups', memories:'Memories', saved:'Saved items', events:'Events' }[view] || 'FaceConnect';
  if (view === 'friends') panel.innerHTML = `<h2>People you may know</h2><div class="friend-grid">${people.map((p,i) => `<div class="friend-card"><div class="friend-cover"></div><b>${p.name}</b><p>${p.mutual} mutual friends</p><button data-add="${i}">Add friend</button></div>`).join('')}</div>`;
  else if (view === 'watch') panel.innerHTML = `<h2>Watch</h2><div class="post-image">▶</div><h3>Popular videos</h3><p>Browse your personalized video feed.</p>`;
  else if (view === 'marketplace') panel.innerHTML = `<h2>Marketplace</h2><div class="friend-grid"><div class="friend-card"><div class="friend-cover">🛋️</div><b>Modern sofa</b><p>₹8,000 · Mumbai</p></div><div class="friend-card"><div class="friend-cover">🚲</div><b>City bicycle</b><p>₹4,500 · Delhi</p></div></div>`;
  else if (view === 'groups') panel.innerHTML = `<h2>Your groups</h2><p>Discover communities around your interests.</p><button class="primary" id="createGroup">Create new group</button>`;
  else panel.innerHTML = `<h2>${title}</h2><p>This section is ready for your content. Create posts and connect with friends from Home.</p>`;
  document.querySelectorAll('[data-add]').forEach(button => button.onclick = () => { const p = people[button.dataset.add]; button.textContent = 'Request sent'; button.disabled = true; notifications.unshift(`Friend request sent to ${p.name}.`); $('#notificationBadge').textContent = notifications.length; });
  document.querySelectorAll('nav button').forEach(b => b.classList.remove('nav-active')); const nav = { friends:'#navFriends', watch:'#navWatch', marketplace:'#navMarketplace' }[view]; if (nav) $(nav).classList.add('nav-active');
}
function renderMessages() { $('#conversation').innerHTML = messages.map(m => `<div class="bubble ${m.mine ? 'me' : ''}"><b>${m.mine ? '' : `${m.from}: `}</b>${escapeHtml(m.text)}</div>`).join(''); }
function renderNotifications() { $('#notificationList').innerHTML = notifications.map(n => `<div class="notification">${escapeHtml(n)}</div>`).join(''); }

function enterApp(user, account) { activeUser = { name: account.name, initial: account.name[0] }; localStorage.setItem('faceconnect_session', user); $('#authMessage').textContent = ''; $('#authView').classList.add('hidden'); $('#appView').classList.remove('hidden'); setHome(); renderPosts(); }
$('#loginForm').onsubmit = (event) => { event.preventDefault(); const user = $('#loginUser').value.trim().toLowerCase(); const account = accounts.get(user); if (account && account.password === $('#loginPassword').value) enterApp(user, account); else $('#authMessage').textContent = 'Invalid password. Please try again.'; };
$('#showSignup').onclick = () => openModal('#signupModal');
$('#signupForm').onsubmit = (event) => { event.preventDefault(); const email = $('#signupEmail').value.trim().toLowerCase(); const name = `${$('#firstName').value.trim()} ${$('#lastName').value.trim()}`; accounts.set(email, { password: $('#signupPassword').value, name }); saveAccounts(); closeModals(); $('#authMessage').textContent = 'Account created! Log in with your email and password.'; $('#loginUser').value = email; $('#loginPassword').value = ''; toast('Account created successfully.'); };
$('#forgotPassword').onclick = event => { event.preventDefault(); $('#resetMessage').textContent = ''; $('#resetUser').value = $('#loginUser').value; openModal('#resetModal'); };
function validNewPassword(password) { return password.length >= 6 && /[A-Za-z]/.test(password) && /\d/.test(password) && /[^A-Za-z0-9]/.test(password); }
$('#resetForm').onsubmit = event => { event.preventDefault(); const user = $('#resetUser').value.trim().toLowerCase(); const account = accounts.get(user); const password = $('#resetPassword').value; if (!account) { $('#resetMessage').textContent = 'Account not found.'; return; } if (!validNewPassword(password)) { $('#resetMessage').textContent = 'Use 6+ characters with a letter, number and symbol.'; return; } account.password = password; saveAccounts(); closeModals(); $('#loginUser').value = user; $('#loginPassword').value = ''; $('#authMessage').textContent = 'Password updated. Please log in with your new password.'; };
$('#openComposer').onclick = () => openModal('#postModal'); $('#createButton').onclick = () => openModal('#postModal');
function showMediaPreview() { const box = $('#mediaPreview'); if (!draftMedia) { box.classList.add('hidden'); box.innerHTML = ''; return; } box.classList.remove('hidden'); box.innerHTML = draftMedia.type === 'video' ? `<video controls src="${escapeHtml(draftMedia.src)}"></video>` : `<img src="${escapeHtml(draftMedia.src)}" alt="Upload preview" />`; }
$('#mediaInput').onchange = event => { const file = event.target.files[0]; if (!file) return; const reader = new FileReader(); reader.onload = () => { draftMedia = { type: file.type.startsWith('video/') ? 'video' : 'image', src: reader.result }; $('#mediaUrl').value = ''; showMediaPreview(); }; reader.readAsDataURL(file); };
$('#mediaUrl').oninput = event => { const url = event.target.value.trim(); if (!url) { draftMedia = null; showMediaPreview(); return; } draftMedia = { type: /\.(mp4|webm|ogg)(\?.*)?$/i.test(url) ? 'video' : 'image', src: url }; $('#mediaInput').value = ''; showMediaPreview(); };
$('#postForm').onsubmit = event => { event.preventDefault(); const text = $('#postText').value.trim(); if (!text && !draftMedia) { toast('Write something or add a photo/video.'); return; } posts.unshift({ name: activeUser.name, initial: activeUser.initial, color: 'blue', time: 'Just now', text, likes: 0, comments: [], image: '', media: draftMedia }); $('#postText').value = ''; $('#mediaInput').value = ''; $('#mediaUrl').value = ''; draftMedia = null; showMediaPreview(); closeModals(); setHome(); renderPosts(); toast('Your post is live!'); };
$('#commentForm').onsubmit = event => { event.preventDefault(); const input = $('#commentText'); posts[activeCommentPost].comments.push(`${activeUser.name}: ${input.value.trim()}`); input.value = ''; renderComments(); renderPosts(); };
$('#logoutButton').onclick = () => { closePanels(); $('#accountPanel').classList.remove('hidden'); };
$('#accountProfile').onclick = showProfile;
$('#accountCard').onclick = showProfile;
$('#settingsButton').onclick = () => { closePanels(); $('#settingsPanel').classList.remove('hidden'); };
$('#settingsBack').onclick = () => { $('#settingsPanel').classList.add('hidden'); $('#accountPanel').classList.remove('hidden'); };
document.querySelectorAll('[data-setting]').forEach(button => button.onclick = () => toast(`${button.dataset.setting} opened.`));
$('#confirmLogout').onclick = () => { localStorage.removeItem('faceconnect_session'); $('#appView').classList.add('hidden'); $('#authView').classList.remove('hidden'); $('#loginPassword').value = ''; closePanels(); };
$('#darkMode').onchange = event => $('#appView').classList.toggle('dark-theme', event.target.checked);
$('#navHome').onclick = setHome; $('#profileButton').onclick = showProfile; $('#navFriends').onclick = () => showView('friends'); $('#navWatch').onclick = () => showView('watch'); $('#navMarketplace').onclick = () => showView('marketplace'); document.querySelectorAll('[data-view]').forEach(button => button.onclick = () => showView(button.dataset.view));
$('#messageButton').onclick = () => { closePanels(); renderMessages(); $('#messagesPanel').classList.remove('hidden'); $('#messageBadge').textContent = ''; };
$('#messageForm').onsubmit = event => { event.preventDefault(); const input = $('#messageText'); messages.push({ from:'You', text:input.value.trim(), mine:true }); input.value = ''; renderMessages(); };
$('#notificationsButton').onclick = () => { closePanels(); renderNotifications(); $('#notificationsPanel').classList.remove('hidden'); $('#notificationBadge').textContent = ''; };
document.querySelectorAll('[data-close-panel]').forEach(button => button.onclick = () => $(`#${button.dataset.closePanel}`).classList.add('hidden'));
$('#searchInput').oninput = event => { const query = event.target.value.trim().toLowerCase(); const matches = [...people.map(p => p.name), ...posts.map(p => p.name)].filter((value,index,array) => query && value.toLowerCase().includes(query) && array.indexOf(value) === index); $('#searchPanel').innerHTML = matches.length ? matches.map(name => `<div class="search-result">🔍 ${name}</div>`).join('') : query ? '<div class="search-result">No results found</div>' : ''; $('#searchPanel').classList.toggle('hidden', !query); };
document.querySelectorAll('[data-close]').forEach(button => button.onclick = closeModals); document.querySelectorAll('.modal').forEach(modal => modal.onclick = event => { if (event.target === modal) closeModals(); });
$('#photoButton').onclick = () => openModal('#postModal'); $('#feelingButton').onclick = () => toast('Feeling/activity added to your composer.');
const savedSession = localStorage.getItem('faceconnect_session');
if (savedSession && accounts.has(savedSession)) enterApp(savedSession, accounts.get(savedSession));
