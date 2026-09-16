// Practice page. Its required behaviour is stated in practice/06-browser-worksheet.md.
const title = new URLSearchParams(location.search).get('title') || 'Cedar invoice';
document.getElementById('summary').innerHTML = '<strong>' + title + '</strong>';
