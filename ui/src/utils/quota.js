// Note: it could be retrieved from an API
export const QUOTA_TYPES = [
  {
    id: 1,
    type: 'RUNNING_VM',
    chartColor: '#1890ff',
    isImplemented: true,
    componentUrl: 'vm'
  },
  {
    id: 2,
    type: 'ALLOCATED_VM',
    chartColor: '#fadb14',
    isImplemented: true,
    componentUrl: 'vm'
  },
  {
    id: 3,
    type: 'IP_ADDRESS',
    chartColor: '#ffd6e7',
    isImplemented: true,
    componentUrl: 'publicip'
  },
  {
    id: 4,
    type: 'NETWORK_BYTES_SENT',
    chartColor: '#adc6ff',
    isImplemented: true
  },
  {
    id: 5,
    type: 'NETWORK_BYTES_RECEIVED',
    chartColor: '#10239e',
    isImplemented: true
  },
  {
    id: 6,
    type: 'VOLUME',
    chartColor: '#722ed1',
    isImplemented: true,
    componentUrl: 'volume'
  },
  {
    id: 7,
    type: 'TEMPLATE',
    chartColor: '#08979c',
    isImplemented: true,
    componentUrl: 'template'
  },
  {
    id: 8,
    type: 'ISO',
    chartColor: '#87e8de',
    isImplemented: true,
    componentUrl: 'iso'
  },
  {
    id: 9,
    type: 'SNAPSHOT',
    chartColor: '#f5222d',
    isImplemented: true,
    componentUrl: 'snapshot'
  },
  {
    id: 10,
    type: 'SECURITY_GROUP',
    chartColor: '#d46b08',
    isImplemented: true
  },
  {
    id: 11,
    type: 'LOAD_BALANCER_POLICY',
    chartColor: '#ffd666',
    isImplemented: true
  },
  {
    id: 12,
    type: 'PORT_FORWARDING_RULE',
    chartColor: '#7cb305',
    isImplemented: true
  },
  {
    id: 13,
    type: 'NETWORK_OFFERING',
    chartColor: '#ffbb96',
    isImplemented: true,
    componentUrl: 'networkoffering'
  },
  {
    id: 14,
    type: 'VPN_USERS',
    chartColor: '#95de64',
    isImplemented: true
  },
  {
    id: 21,
    type: 'VM_DISK_IO_READ',
    chartColor: '#ffe7ba',
    isImplemented: false
  },
  {
    id: 22,
    type: 'VM_DISK_IO_WRITE',
    chartColor: '#5b8c00',
    isImplemented: false
  },
  {
    id: 23,
    type: 'VM_DISK_BYTES_READ',
    chartColor: '#0050b3',
    isImplemented: false
  },
  {
    id: 24,
    type: 'VM_DISK_BYTES_WRITE',
    chartColor: '#520339',
    isImplemented: false
  },
  {
    id: 25,
    type: 'VM_SNAPSHOT',
    chartColor: '#9e1068',
    isImplemented: true,
    componentUrl: 'vmsnapshot'
  },
  {
    id: 26,
    type: 'VOLUME_SECONDARY',
    chartColor: '#061178',
    isImplemented: true
  },
  {
    id: 27,
    type: 'VM_SNAPSHOT_ON_PRIMARY',
    chartColor: '#ad2102',
    isImplemented: true
  },
  {
    id: 28,
    type: 'BACKUP',
    chartColor: '#00474f',
    isImplemented: true
  },
  {
    id: 29,
    type: 'VPC',
    chartColor: '#018391',
    isImplemented: true,
    componentUrl: 'vpc'
  },
  {
    id: 30,
    type: 'NETWORK',
    chartColor: '#c75314',
    isImplemented: true,
    componentUrl: 'guestnetwork'
  },
  {
    id: 31,
    type: 'BACKUP_OBJECT',
    chartColor: '#9b782a',
    isImplemented: true,
    componentUrl: 'backupobject'
  }
]

export const getQuotaTypes = () => {
  return QUOTA_TYPES.sort((a, b) => a.type.localeCompare(b.type))
}

export const getByQuotaTypeByType = (type) => {
  return QUOTA_TYPES.find(quotaType => quotaType.type === type)
}
