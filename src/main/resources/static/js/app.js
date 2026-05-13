(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {

        initThemeMode();
        initThemeToggle();
        initSidebarState();
        initSidebarToggle();
        initStatusForms();
        initOverrideDateToggle();
        initConfirmDialogs();
        initTableSearch();
    });

    function initThemeMode() {
        var root = document.documentElement;
        var storedTheme = null;

        try {
            storedTheme = localStorage.getItem('cronos-theme');
        } catch (error) {
            storedTheme = null;
        }

        var preferredTheme = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
            ? 'dark'
            : 'light';
        root.dataset.theme = storedTheme || preferredTheme;
    }

    function initThemeToggle() {
        var button = document.querySelector('[data-theme-toggle]');
        if (!button) return;

        var label = button.querySelector('[data-theme-label]');
        var root = document.documentElement;

        syncThemeToggleState(button, label, root.dataset.theme || 'light');

        button.addEventListener('click', function() {
            var nextTheme = root.dataset.theme === 'dark' ? 'light' : 'dark';
            root.dataset.theme = nextTheme;
            syncThemeToggleState(button, label, nextTheme);

            try {
                localStorage.setItem('cronos-theme', nextTheme);
            } catch (error) {
                // No persistence available.
            }
        });
    }

    function syncThemeToggleState(button, label, theme) {
        var isDark = theme === 'dark';
        var nextLabel = isDark ? 'Claro' : 'Oscuro';

        button.setAttribute('aria-pressed', String(isDark));
        button.setAttribute('aria-label', isDark ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro');

        if (label) {
            label.textContent = nextLabel;
        }
    }

    function initSidebarState() {
        var frame = document.querySelector('.app-frame');
        if (!frame) return;

        var collapsed = false;

        try {
            collapsed = localStorage.getItem('cronos-sidebar-collapsed') === 'true';
        } catch (error) {
            collapsed = false;
        }

        frame.classList.toggle('sidebar-collapsed', collapsed);
    }

    function initSidebarToggle() {
        var button = document.querySelector('[data-sidebar-toggle]');
        var frame = document.querySelector('.app-frame');
        if (!button || !frame) return;

        button.addEventListener('click', function() {
            var collapsed = !frame.classList.contains('sidebar-collapsed');
            frame.classList.toggle('sidebar-collapsed', collapsed);

            try {
                localStorage.setItem('cronos-sidebar-collapsed', String(collapsed));
            } catch (error) {
                // No persistence available.
            }
        });
    }

    function initStatusForms() {
        document.querySelectorAll('.status-form select').forEach(function(select) {
            select.addEventListener('change', function() {
                this.closest('form').submit();
            });
        });
    }

    function initOverrideDateToggle() {
        var overrideInput = document.querySelector('input[name="dueDateOverride"]');
        var reasonGroup = document.querySelector('.override-reason');
        if (overrideInput && reasonGroup) {
            overrideInput.addEventListener('change', function() {
                reasonGroup.style.display = this.value ? 'block' : 'none';
            });
            if (overrideInput.value) {
                reasonGroup.style.display = 'block';
            }
        }
    }

    function initConfirmDialogs() {
        document.querySelectorAll('[data-confirm]').forEach(function(el) {
            el.addEventListener('click', function(e) {
                if (!confirm(this.getAttribute('data-confirm'))) {
                    e.preventDefault();
                }
            });
        });
    }

    function initTableSearch() {
        var searchInput = document.getElementById('tableSearch');
        if (!searchInput) return;

        searchInput.addEventListener('input', function() {
            var query = this.value.toLowerCase();
            var table = document.querySelector('.data-table');
            if (!table) return;

            table.querySelectorAll('tbody tr').forEach(function(row) {
                var text = row.textContent.toLowerCase();
                row.style.display = text.includes(query) ? '' : 'none';
            });
        });
    }

    window.toggleDropdown = function(event, btn) {
        event.stopPropagation();
        var wrapper = btn.closest('.more-obligations-wrapper');
        var wasOpen = wrapper.classList.contains('open');
        document.querySelectorAll('.more-obligations-wrapper.open').forEach(function(el) {
            el.classList.remove('open');
        });
        if (!wasOpen) {
            wrapper.classList.add('open');
        }
    };

    window.openTaskPopup = function(btn) {
        var id = btn.getAttribute('data-id');
        var client = btn.getAttribute('data-client');
        var type = btn.getAttribute('data-type');
        var status = btn.getAttribute('data-status');
        var tone = btn.getAttribute('data-tone');
        
        var popup = document.getElementById('task-popup');
        if (!popup) return;
        
        popup.querySelector('.task-popup-title').textContent = type;
        popup.querySelector('.task-popup-subtitle').textContent = client;
        popup.querySelector('.task-popup-status').textContent = status;
        popup.querySelector('.task-popup-status').className = 'pill tone-' + tone + ' task-popup-status';
        
        var detailsBtn = popup.querySelector('.task-popup-details-btn');
        if (detailsBtn) {
            detailsBtn.href = '/obligaciones/' + id;
        }
        
        popup.classList.add('active');
    };

    window.closeTaskPopup = function() {
        var popup = document.getElementById('task-popup');
        if (popup) popup.classList.remove('active');
    };

    document.addEventListener('click', function(event) {
        if (!event.target.closest('.more-obligations-wrapper')) {
            document.querySelectorAll('.more-obligations-wrapper.open').forEach(function(el) {
                el.classList.remove('open');
            });
        }
    });

})();
