# Generated by Django 4.0.1 on 2022-01-26 02:00

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ('users', '0001_initial'),
    ]

    operations = [
        migrations.CreateModel(
            name='DriverProfile',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('v_type', models.CharField(max_length=50)),
                ('v_capacity', models.IntegerField(default=1)),
                ('v_platenumber', models.CharField(max_length=7)),
                ('licensenum', models.CharField(max_length=12)),
                ('special_vinfo', models.TextField(blank=b'I01\n', max_length=200)),
                ('image', models.ImageField(default='default.jpg', upload_to='profile_pics')),
                ('user', models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.DeleteModel(
            name='Driver',
        ),
    ]
