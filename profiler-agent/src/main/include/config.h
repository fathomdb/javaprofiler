// On some systems (like freebsd), we can't call write() at all in a
// global constructor, perhaps because errno hasn't been set up.
// (In windows, we can't call it because it might call malloc.)
// Calling the write syscall is safer (it doesn't set errno), so we
// prefer that.  Note we don't care about errno for logging: we just
// do logging on a best-effort basis.
#if defined(_MSC_VER)
#define WRITE_TO_STDERR(buf, len) WriteToStderr(buf, len);  // in port.cc
#elif defined(HAVE_SYS_SYSCALL_H)
#include <sys/syscall.h>
#define WRITE_TO_STDERR(buf, len) syscall(SYS_write, STDERR_FILENO, buf, len)
#else
#define WRITE_TO_STDERR(buf, len) write(STDERR_FILENO, buf, len)
#endif

// TODO(jandrews): Also print the values in case of failure.  Requires some
// sort of type-sensitive ToString() function.
#define CHECK_OP(op, val1, val2)                                        \
  do {                                                                  \
    if (!((val1) op (val2))) {                                          \
      fprintf(stderr, "Check failed: %s %s %s\n", #val1, #op, #val2);   \
      abort();                                                          \
    }                                                                   \
  } while (0)

#define CHECK_EQ(val1, val2) CHECK_OP(==, val1, val2)
#define CHECK_NE(val1, val2) CHECK_OP(!=, val1, val2)
#define CHECK_LE(val1, val2) CHECK_OP(<=, val1, val2)
#define CHECK_LT(val1, val2) CHECK_OP(< , val1, val2)
#define CHECK_GE(val1, val2) CHECK_OP(>=, val1, val2)
#define CHECK_GT(val1, val2) CHECK_OP(> , val1, val2)


// This takes a message to print.  The name is historical.
#define RAW_CHECK(condition, message)                                          \
  do {                                                                         \
    if (!(condition)) {                                                        \
      WRITE_TO_STDERR("Check failed: " #condition ": " message "\n",           \
                      sizeof("Check failed: " #condition ": " message "\n")-1);\
      abort();                                                                 \
    }                                                                          \
  } while (0)


#define PRIuS "lu"

#define HAVE_UNISTD_H (1)

// A macro to disallow the evil copy constructor and operator= functions
// This should be used in the private: declarations for a class
#define DISALLOW_EVIL_CONSTRUCTORS(TypeName)    \
  TypeName(const TypeName&);                    \
  void operator=(const TypeName&)

// An alternate name that leaves out the moral judgment... :-)
#define DISALLOW_COPY_AND_ASSIGN(TypeName) DISALLOW_EVIL_CONSTRUCTORS(TypeName)

