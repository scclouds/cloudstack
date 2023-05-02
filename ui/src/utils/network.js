import { api } from '@/api'

export async function getAvailablePublicIpAddresses (zoneid, domainid, account) {
  const apiResult = await new Promise((resolve, reject) => {
    const params = {
      zoneid,
      domainid,
      account,
      forvirtualnetwork: true,
      allocatedonly: false
    }
    api('listPublicIpAddresses', params).then(json => {
      const listPublicIps = json.listpublicipaddressesresponse.publicipaddress || []
      resolve(listPublicIps)
    }).catch(reject)
  })

  const listPublicIpAddress = apiResult.filter(item => item.state === 'Free')

  listPublicIpAddress.sort(function (a, b) {
    if (a.ipaddress < b.ipaddress) { return -1 }
    if (a.ipaddress > b.ipaddress) { return 1 }
    return 0
  })

  return listPublicIpAddress
}
