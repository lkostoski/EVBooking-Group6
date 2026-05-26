let mapsPromise = null;

export async function loadGoogleMaps() {
  if (mapsPromise) return mapsPromise;

  mapsPromise = (async () => {
    const cfg = await fetch('api/config', { credentials: 'include' }).then(r => r.json());
    if (!cfg.mapsKey) throw new Error('No Maps key configured');

    return new Promise((resolve, reject) => {
      const cbName = '__gmapsReady_' + Date.now();
      const script = document.createElement('script');
      script.src = `https://maps.googleapis.com/maps/api/js?key=${cfg.mapsKey}&callback=${cbName}`;
      script.async = true;
      script.defer = true;
      window[cbName] = () => { delete window[cbName]; resolve(window.google.maps); };
      script.onerror = (e) => { mapsPromise = null; reject(e); };
      document.head.appendChild(script);
    });
  })();

  return mapsPromise;
}

const PIN_PATH = 'M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z';

export function markerIcon(selected = false) {
  return {
    path: PIN_PATH,
    fillColor: selected ? '#22c55e' : '#16a34a',
    fillOpacity: 1,
    strokeColor: '#ffffff',
    strokeWeight: selected ? 4 : 2,
    scale: selected ? 2.2 : 1.6,
    anchor: new google.maps.Point(12, 22),
  };
}

export function createStationMarker(map, station, onClick) {
  const marker = new google.maps.Marker({
    position: { lat: station.latitude, lng: station.longitude },
    map,
    title: station.name,
    icon: markerIcon(false),
  });

  if (onClick) marker.addListener('click', (e) => { e.stop(); onClick(station, marker); });
  return marker;
}
