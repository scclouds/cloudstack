import * as momentLib from 'moment'
import store from '@/store/'

export const formats = {
  DATE_EXTENDED: 'MMM DD, YYYY',
  DATETIME_EXTENDED: 'MMM DD, YYYY, HH:mm:ssZ',
  ISO_DATE_ONLY: 'YYYY-MM-DD'
}

export function formatDateToExtended (dateInMoment) {
  return dateInMoment.format(formats.DATE_EXTENDED)
}

export function formatDateToIsoDate (dateInMoment) {
  return dateInMoment.format(formats.ISO_DATE_ONLY)
}

export function formatDatetimeToExtended (dateInMoment) {
  return dateInMoment.format(formats.DATETIME_EXTENDED)
}

export function getMomentFormattedAndNormalized ({ value, keepMoment = true, format = true }) {
  if (typeof value === 'string') {
    value = moment(value)
  }

  if (!store.getters.usebrowsertimezone) {
    value = value.utc(keepMoment)
  }

  if (format) {
    return value.format()
  } else {
    return value
  }
}

/**
 * When passing a string/moment to the date picker component, it converts the value to the browser timezone; therefore,
 * we need to normalize the value to UTC if user is not using browser's timezone.
 * @param {*} value The datetime to normalize.
 * @returns A moment object with the datetime normalized to UTC if user is not using browser's timezone;
 * otherwise, a correspondent moment object based on the value passed.
 */
export function getMomentNormalizedToDatePicker (value) {
  if (!value) {
    return undefined
  }

  if (typeof value === 'string') {
    value = moment(value)
  }

  var timezoneOffset = 0
  if (!store.getters.usebrowsertimezone) {
    timezoneOffset = new Date().getTimezoneOffset() / 60
  }

  return moment(value).add(timezoneOffset, 'hours')
}

export function toLocalDate ({ date, timezoneoffset = store.getters.timezoneoffset, usebrowsertimezone = store.getters.usebrowsertimezone }) {
  if (usebrowsertimezone) {
    // Since GMT+530 is returned as -330 (mins to GMT)
    timezoneoffset = new Date().getTimezoneOffset() / -60
  }

  const milliseconds = Date.parse(date)
  // e.g. "Tue, 08 Jun 2010 19:13:49 GMT", "Tue, 25 May 2010 12:07:01 UTC"
  return new Date(milliseconds + (timezoneoffset * 60 * 60 * 1000))
}

export function toLocaleDate ({ date, timezoneoffset = store.getters.timezoneoffset, usebrowsertimezone = store.getters.usebrowsertimezone, dateOnly = false, hourOnly = false }) {
  if (!date) {
    return undefined
  }

  var dateWithOffset = toLocalDate({ date, timezoneoffset, usebrowsertimezone }).toUTCString()
  // e.g. "08 Jun 2010 19:13:49 GMT", "25 May 2010 12:07:01 UTC"
  dateWithOffset = dateWithOffset.substring(dateWithOffset.indexOf(', ') + 2)
  // e.g. "08 Jun 2010 19:13:49", "25 May 2010 12:10:16"
  dateWithOffset = dateWithOffset.substring(0, dateWithOffset.length - 4)

  if (dateOnly) {
    return dateWithOffset.substring(0, dateWithOffset.length - 9)
  }

  if (hourOnly) {
    return dateWithOffset.substring(dateWithOffset.length - 8, dateWithOffset.length)
  }

  return dateWithOffset
}

export const moment = momentLib
