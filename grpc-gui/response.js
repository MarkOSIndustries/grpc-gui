module.exports = function(channels) {
  const cmd = {
  }

  const dom = {
    statusArea: document.querySelector('#status-area'),
    responseTiming: document.querySelector('#response-timing'),
    responseError: document.querySelector('#response-error'),
    responseOutcome: document.querySelector('#response-outcome'),
  }

  const renderers = [
    require('./response.log')(document.querySelector('#response-listing-log')),
    require('./response.table')(document.querySelector('#response-listing-table')),
  ]

  const state = {
    inFlight: false,
    responses: [],
    status: '',
  }

  document.querySelectorAll('input[name=render-mode]').forEach(renderMode => {
    renderMode.addEventListener('change', () => {
      console.log('Repsonse listing mode changed to ', renderMode.value)
      document.querySelectorAll(`.response-listing:not(#response-listing-${renderMode.value})`).forEach(listingMode => listingMode.classList.add('hidden'))
      document.querySelector(`#response-listing-${renderMode.value}`).classList.remove('hidden')
      localStorage.setItem('last-render-mode', renderMode.value)
    })
  })
  document.querySelector(`input#render-mode-${localStorage.getItem('last-render-mode') || 'log'}`).click()

  channels.invocation.subject('started').subscribe(requestPayload => {
    renderers.forEach(renderer => renderer.reset())
    dom.responseOutcome.innerHTML = 'Running'
    dom.responseTiming.innerHTML = '...'
    dom.responseError.innerHTML = ''
    dom.responseError.classList.add('hidden')

    state.inFlight = true
    state.error = null
    state.responses = []
    state.status = 'success'

    const t0 = performance.now()
    const durationRenderInterval = setInterval(() => {
      const t1 = performance.now()
      const duration = t1-t0
      dom.responseTiming.innerHTML = `${duration.toFixed(3)} milliseconds`
      if(!state.inFlight) {
        clearInterval(durationRenderInterval)
        console.log('Response completed in', duration)
      }
    }, 25)
    const responseRenderInterval = setInterval(() => {
      if(!state.inFlight) {
        clearInterval(responseRenderInterval)
      }

      if(state.responses.length) {
        renderers.forEach(renderer => renderer.render(state.responses))
        state.responses = []
      }
    }, 500)
  })
  channels.invocation.subject('received').subscribe(response => {
    state.responses.push(response)
  })
  channels.invocation.subject('error').subscribe(error => {
    state.status = 'failure'
    state.error = error
  })
  channels.invocation.subject('finished').subscribe(() => {
    state.inFlight = false
    dom.responseOutcome.innerHTML = state.status
    dom.statusArea.setAttribute('data-state', state.status)
    if(state.error) {
      dom.responseError.innerHTML = `<pre>${JSON.stringify(state.error, undefined, '  ')}</pre>`
      dom.responseError.classList.remove('hidden')
    }
  })

  return cmd
}
