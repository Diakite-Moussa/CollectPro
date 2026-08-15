import { TestBed, ComponentFixture } from '@angular/core/testing';
import { FormBuilderComponent } from './form-builder.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('FormBuilderComponent', () => {
  let component: FormBuilderComponent;
  let fixture: ComponentFixture<FormBuilderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormBuilderComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(FormBuilderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create form builder component', () => {
    expect(component).toBeTruthy();
    expect(component.fields.length).toBe(0);
  });

  it('should add new field when addField is called', () => {
    component.addField();
    expect(component.fields.length).toBe(1);

    const field = component.fields[0];
    expect(field.label).toContain('Nouveau champ');
    expect(field.type).toBe('text');
    expect(field.required).toBe(false);
  });

  it('should remove field when removeField is called', () => {
    component.addField();
    component.addField();
    expect(component.fields.length).toBe(2);

    component.removeField(0);
    expect(component.fields.length).toBe(1);
  });
});
