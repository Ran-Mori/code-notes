## interrupts

### hardward interrupt - press keyboard

* **value to cpu**: The usb controller receives the scan code. It immediately notifies a chip call **Programmable Interrupt Controller** who's job is to manage the hardward interrupt. It signals the cpu on a specific physical pins issueing an **Interruput Request**. And the keyboard has a pre-assigned **IRQ** number.
* **enter the kernel**: cpu saves user space mode context and then jump to kernel mode. The CPU uses the IRQ number as an index to look into a special table in memory called the IVT. This table, which the Operating System created at boot time, is an "address book" that maps interrupt numbers to the memory addresses of the functions designed to handle them. the code will run is called **Interrupt Service Routine (ISR)**
* **zero cost**: Polling is not efficient if you check every time. However, the CPU can monitor whether there is an interruption every time it finish executing an instruction. This mechanism is hardware-level and has zero cost.
* **top half** of hardware interrupt: ISR quickly places this raw scan code into a kernel buffer. And then back to user space mode. Because the CPU pauses almost everything when a hardware interrupt occurs, and cannot respond to other interrupts at this time, the execution efficiency of the ISR must be very high. Therefore, for keyboard events, the code value can be stored in the buffer.
* **bottom half**: A moment later, when the CPU is less busy, the OS scheduler runs a lower-priority kernel task (the "bottom half"). It can be implemented by a separate kernel thread. Its task is to pass the code value in the kernel buffer to the application currently in the foreground. This is connected to Android's InputMangerService

### soft interrupt - system call

* it is the process **talking to the kernel**.
* it is a user program deliberately knocking on the kernel's door to ask for help by system call.