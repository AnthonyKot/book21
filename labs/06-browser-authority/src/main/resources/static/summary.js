// Deliberately unfinished practice: render the title as text inside a strong element.
const title = new URLSearchParams(location.search).get('title') || 'Cedar invoice';
document.getElementById('summary').innerHTML = '<strong>' + title + '</strong>';
