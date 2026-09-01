import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  AfterViewInit,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import * as L from 'leaflet';

export interface MapMarker {
  id: number;
  lat: number;
  lng: number;
  label: string;
  radiusMeters?: number;
  color?: string;
}

/** Niveau de zoom appliqué quand on clique sur un marqueur. */
const MARKER_CLICK_ZOOM = 15;

// Fix des icônes par défaut Leaflet cassées avec le bundler Angular.
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'leaflet/marker-icon-2x.png',
  iconUrl: 'leaflet/marker-icon.png',
  shadowUrl: 'leaflet/marker-shadow.png',
});

@Component({
  selector: 'app-map-view',
  standalone: true,
  template: `<div #mapContainer class="map-container"></div>`,
  styles: [`
    .map-container {
      width: 100%;
      height: 500px;
      border-radius: 8px;
    }
  `],
})
export class MapViewComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef<HTMLDivElement>;

  @Input() markers: MapMarker[] = [];
  @Input() defaultCenter: [number, number] = [12.6392, -8.0029]; // Bamako par défaut
  @Input() defaultZoom = 6;

  private map: L.Map | null = null;
  private layerGroup: L.LayerGroup | null = null;
  private viewInitialized = false;

  ngAfterViewInit(): void {
    this.map = L.map(this.mapContainer.nativeElement).setView(
      this.defaultCenter,
      this.defaultZoom,
    );

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(this.map);

    this.layerGroup = L.layerGroup().addTo(this.map);
    this.viewInitialized = true;
    this.drawMarkers();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['markers'] && this.viewInitialized) {
      this.drawMarkers();
    }
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = null;
  }

  private drawMarkers(): void {
    if (!this.map || !this.layerGroup) return;

    this.layerGroup.clearLayers();

    if (this.markers.length === 0) return;

    const bounds: L.LatLngExpression[] = [];

    for (const marker of this.markers) {
      const latLng: L.LatLngExpression = [marker.lat, marker.lng];
      bounds.push(latLng);

      const leafletMarker = L.marker(latLng).bindPopup(marker.label);
      // Clic sur un marqueur -> zoom + centrage dédié (en plus du popup).
      leafletMarker.on('click', () => {
        this.map?.setView(latLng, MARKER_CLICK_ZOOM, { animate: true });
      });
      this.layerGroup.addLayer(leafletMarker);

      if (marker.radiusMeters && marker.radiusMeters > 0) {
        const circle = L.circle(latLng, {
          radius: marker.radiusMeters,
          color: marker.color ?? '#3388ff',
          fillColor: marker.color ?? '#3388ff',
          fillOpacity: 0.15,
        });
        this.layerGroup.addLayer(circle);
      }
    }

    if (bounds.length === 1) {
      this.map.setView(bounds[0], 14);
    } else if (bounds.length > 1) {
      this.map.fitBounds(L.latLngBounds(bounds), { padding: [40, 40] });
    }
  }
}
