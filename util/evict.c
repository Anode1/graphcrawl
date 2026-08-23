/* evict.c -- drop a file's pages from the page cache, so a timing can be
 * taken cold without root. posix_fadvise(DONTNEED) on clean pages.
 *
 *   evict FILE...
 */
#define _POSIX_C_SOURCE 200809L
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char **argv)
{
    int i, rc = 0;

    for (i = 1; i < argc; i++) {
        int fd = open(argv[i], O_RDONLY);
        if (fd < 0) {
            perror(argv[i]);
            rc = 1;
            continue;
        }
        fdatasync(fd);
        if (posix_fadvise(fd, 0, 0, POSIX_FADV_DONTNEED) != 0) {
            perror(argv[i]);
            rc = 1;
        }
        close(fd);
    }
    return rc;
}
