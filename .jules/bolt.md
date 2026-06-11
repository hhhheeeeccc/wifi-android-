## 2025-05-14 - [Main Thread Blocking via Handler]
**Learning:** Using Handler(Looper.getMainLooper()).postDelayed() for periodic tasks that involve file I/O (like reading /proc/net/arp) or shell command execution (like iptables via su) blocks the main thread, causing UI stuttering and potentially ANRs.
**Action:** Always offload I/O and shell command execution to a background thread using ScheduledExecutorService or similar threading mechanisms, and post results back to the UI thread only for view updates.
