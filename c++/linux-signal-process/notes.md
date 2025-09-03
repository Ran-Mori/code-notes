## signal

### what?	

* It is the kernel or another process **talking to the process**.

### process 

* process A runs `kill -signal_name process_B_pid`
* The kernel receives this and marks the SIGINT signal as "pending" for process B. Process B **continues** running for now; it is not interrupted immediately.
* The kernel waits for the next time your process is about to switch from Kernel Mode back to User Mode. This happens right after a **hardware interrupt** (like the timer tick) has been handled, or right after a **software interrupt** (a system call) has finished.
* the kernel **alters the pc register**. Instead of setting the CPU's instruction pointer back to where your program left off, it sets the instruction pointer to the beginning of your program's registered **signal handler** for SIGINT.
* process B resumes in User Mode, but it's been "tricked." Instead of continuing its normal work, it immediately starts executing the signal handler code. When the handler finishes, **control is returned to the kernel**. 
* the kernel then setting the CPU's instruction pointer back to where your program left off