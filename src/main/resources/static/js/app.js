(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {

        initThemeToggle();
        initSidebarState();
        initSidebarToggle();
        initStatusForms();
        initOverrideDateToggle();
        initConfirmDialogs();
        initTableSearch();
        initTopbarAutoHide();
        initUserDropdown();
        initMobileSidebar();
    });

    function initThemeToggle() {
        var button = document.querySelector('[data-theme-toggle]');
        if (!button) return;

        var label = button.querySelector('[data-theme-label]');
        var root = document.documentElement;
        var currentTheme = root.dataset.theme;

        // Fallback: read from localStorage if data-theme wasn't set yet
        if (!currentTheme) {
            try {
                currentTheme = localStorage.getItem('cronos-theme');
            } catch (error) {
                currentTheme = null;
            }
            if (!currentTheme) {
                currentTheme = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
                    ? 'dark' : 'light';
            }
            root.dataset.theme = currentTheme;
        }

        syncThemeToggleState(button, label, currentTheme);

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

    /* ===== Topbar auto-hide on scroll ===== */
    function initTopbarAutoHide() {
        var topbar = document.querySelector('.app-topbar');
        if (!topbar) return;

        var lastScrollY = 0;
        var ticking = false;

        window.addEventListener('scroll', function() {
            if (!ticking) {
                window.requestAnimationFrame(function() {
                    var currentScrollY = window.pageYOffset || document.documentElement.scrollTop;

                    if (currentScrollY > lastScrollY && currentScrollY > 140) {
                        topbar.classList.add('topbar-hidden');
                    } else if (currentScrollY < lastScrollY - 40 || currentScrollY <= 140) {
                        topbar.classList.remove('topbar-hidden');
                    }

                    lastScrollY = Math.max(0, currentScrollY);
                    ticking = false;
                });
                ticking = true;
            }
        });
    }

    /* ===== User dropdown in navbar ===== */
    function initUserDropdown() {
        var trigger = document.querySelector('[data-user-menu]');
        if (!trigger) return;

        var wrapper = trigger.closest('.user-chip-wrapper');
        if (!wrapper) return;

        trigger.addEventListener('click', function(e) {
            e.stopPropagation();
            var isOpen = wrapper.classList.toggle('is-open');
            trigger.setAttribute('aria-expanded', String(isOpen));
        });

        document.addEventListener('click', function(e) {
            if (!wrapper.contains(e.target)) {
                wrapper.classList.remove('is-open');
                trigger.setAttribute('aria-expanded', 'false');
            }
        });
    }

    /* ===== Mobile sidebar drawer ===== */
    function initMobileSidebar() {
        var menuBtn = document.querySelector('[data-mobile-menu]');
        var closeBtn = document.querySelector('[data-sidebar-close]');
        var overlay = document.querySelector('[data-sidebar-overlay]');
        var sidebar = document.querySelector('.app-sidebar');
        var frame = document.querySelector('.app-frame');
        if (!menuBtn || !sidebar) return;

        function closeSidebar() {
            frame.classList.remove('mobile-sidebar-open');
            document.body.style.overflow = '';
        }

        menuBtn.addEventListener('click', function() {
            frame.classList.toggle('mobile-sidebar-open');
            document.body.style.overflow = frame.classList.contains('mobile-sidebar-open') ? 'hidden' : '';
        });

        if (closeBtn) {
            closeBtn.addEventListener('click', closeSidebar);
        }

        if (overlay) {
            overlay.addEventListener('click', function() {
                frame.classList.remove('mobile-sidebar-open');
                document.body.style.overflow = '';
            });
        }

        // Close on navigation link click (mobile)
        sidebar.querySelectorAll('.sidebar-link').forEach(function(link) {
            link.addEventListener('click', function() {
                if (window.innerWidth <= 900) {
                    frame.classList.remove('mobile-sidebar-open');
                    document.body.style.overflow = '';
                }
            });
        });
    }

    /* ===== Calendar dropdown & popup ===== */
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
        var dueDate = btn.getAttribute('data-duedate') || '—';
        var period = btn.getAttribute('data-period') || '—';
        var year = btn.getAttribute('data-year') || '—';
        var priority = btn.getAttribute('data-priority') || '—';
        var priorityTone = btn.getAttribute('data-prioritytone') || 'neutral';
        var responsible = btn.getAttribute('data-responsible') || '—';
        var notes = btn.getAttribute('data-notes');
        
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

        popup.querySelector('#popup-duedate').textContent = dueDate;
        popup.querySelector('#popup-period').textContent = period;
        popup.querySelector('#popup-year').textContent = year;
        
        var priorityEl = popup.querySelector('#popup-priority');
        priorityEl.textContent = priority;
        priorityEl.className = 'detail-value';
        if (priority === 'Alta') priorityEl.classList.add('tone-danger');
        else if (priority === 'Media') priorityEl.classList.add('tone-warning');
        else if (priority === 'Baja') priorityEl.classList.add('tone-success');

        popup.querySelector('#popup-responsible').textContent = responsible;
        
        var notesEl = popup.querySelector('#popup-notes');
        if (notes && notes.trim() !== '') {
            notesEl.textContent = notes;
        } else {
            notesEl.textContent = 'Sin observaciones';
        }
        
        popup.classList.add('active');
    };

    window.closeTaskPopup = function() {
        var popup = document.getElementById('task-popup');
        if (popup) popup.classList.remove('active');
    };

    window.openDayPopup = function(btn) {
        var dayNum = btn.getAttribute('data-day');
        var article = btn.closest('.calendar-day');
        var template = article.querySelector('template');
        if (!template) return;

        var dataContainer = template.content.querySelector('.day-obligations-data');
        if (!dataContainer) return;

        var items = dataContainer.querySelectorAll('.obl-data');
        var popup = document.getElementById('day-popup');
        var list = document.getElementById('day-popup-list');
        if (!popup || !list) return;

        popup.querySelector('.day-popup-title').textContent = 'Día ' + dayNum;
        popup.querySelector('.day-popup-subtitle').textContent = items.length + (items.length === 1 ? ' obligación asignada' : ' obligaciones asignadas');

        list.innerHTML = '';
        items.forEach(function(item) {
            var card = document.createElement('button');
            card.className = 'day-popup-item';
            card.setAttribute('data-id', item.getAttribute('data-id'));
            card.setAttribute('data-client', item.getAttribute('data-client'));
            card.setAttribute('data-type', item.getAttribute('data-type'));
            card.setAttribute('data-status', item.getAttribute('data-status'));
            card.setAttribute('data-tone', item.getAttribute('data-tone'));
            card.setAttribute('data-duedate', item.getAttribute('data-duedate'));
            card.setAttribute('data-period', item.getAttribute('data-period'));
            card.setAttribute('data-year', item.getAttribute('data-year'));
            card.setAttribute('data-priority', item.getAttribute('data-priority'));
            card.setAttribute('data-prioritytone', item.getAttribute('data-prioritytone'));
            card.setAttribute('data-responsible', item.getAttribute('data-responsible'));
            card.setAttribute('data-notes', item.getAttribute('data-notes'));
            card.onclick = function() { closeDayPopup(); openTaskPopup(card); };

            var tone = item.getAttribute('data-tone') || 'neutral';
            card.innerHTML =
                '<div class="day-popup-item-info">' +
                    '<span class="day-popup-item-client">' + (item.getAttribute('data-client') || '') + '</span>' +
                    '<span class="day-popup-item-type">' + (item.getAttribute('data-type') || '') + '</span>' +
                '</div>' +
                '<span class="pill tone-' + tone + ' day-popup-item-status">' + (item.getAttribute('data-status') || '') + '</span>';
            list.appendChild(card);
        });

        popup.classList.add('active');
    };

    window.closeDayPopup = function() {
        var popup = document.getElementById('day-popup');
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
