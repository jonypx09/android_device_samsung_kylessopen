#!/system/bin/sh
# modified by majonez
# thx to daniel

LOG_TAG="CSR7820"
BTUART_PORT=/dev/ttyHS0
BTSTATE=/sys/class/rfkill/rfkill0/state
PSCONFIG=/etc/PSConfig_7820_rev26.psr

logi ()
{
  /system/bin/log -t $LOG_TAG -p i ": $@"
}

loge ()
{
  /system/bin/log -t $LOG_TAG -p e ": $@"
}

failed ()
{
  loge "$1: exit code $2"
  exit $2
}

#Enable power of csr chip
echo "1" > $BTSTATE

# PS Config with bccmd
logwrapper /system/bin/bccmd -t bcsp -d $BTUART_PORT -b 115200 psload -r $PSCONFIG
case $? in
  0) logi "bccmd init port....done";;
  *) failed "port: $BTUART_PORT - bccmd failed" $?;
     exit $?;;
esac

# attach HCI 
logwrapper /system/bin/hciattach -p $BTUART_PORT bcsp 3000000 flow
case $? in
  0) logi "hci attached to : $BTUART_PORT";;
  *) failed "port: $BTUART_PORT - hciattach failed" $?;
     exit $?;;
esac

exit 0